package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.OpCode._
import org.lipicalabs.lipica.core.vm.program.{MessageType, MessageCall, Program}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Lipica VMを表すクラスです。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class VM {

	import VM._

	/** このVM上で実行されたステップ数のカウンタ。 */
	private var vmCounter: Long = 0L

	def play(program: Program): Unit = {
		try {
			while (!program.isStopped) {
				this.step(program)
			}
		} catch {
			case e: RuntimeException =>
				program.setRuntimeFailure(e)
			case stackOverFlowError: StackOverflowError =>
				logger.error("\n !!! StackOverflowError: update your java run command with -Xss32M !!!\n")
				System.exit(-1)
		}
	}

	def step(program: Program): Unit = {
		if (NodeProperties.CONFIG.vmTrace) {
			program.saveOpTrace()
		}
		try {
			//次に実行すべき１バイトに対応する。オペコードオブジェクトを取得する。
			val op = OpCode.code(program.getCurrentOp).orNull
			if (op eq null) {
				throw Program.Exception.invalidOpCode(program.getCurrentOp)
			}

			//文脈情報を保持し、スタックの深さを確認する。
			program.setLastOp(op.opcode)
			program.verifyStackSize(op.require)
			program.verifyStackOverflow(op.require, op.ret)

			val manaBefore = program.getMana.longValue
			//必要なマナ容量および新たに必要となるメモリサイズを計算する。
			val (baseManaCost, newMemSize, copySize) = computeNecessaryResources(op, program)
			//マナを消費する。
			program.spendMana(baseManaCost, op.toString)

			if (MaxMana < newMemSize) {
				throw Program.Exception.manaOverflow(newMemSize, MaxMana)
			}

			//マナコストを、さらに詳しく計算する。
			var manaCost = baseManaCost
			//このステップにおけるメモリ使用の増分に対する課金を計算する。
			//まずWord単位で切り上げる。
			val newMemoryWords = DataWord.countWords(newMemSize.longValue())
			val newMemoryUsage = newMemoryWords * DataWord.NUM_BYTES
			//前ステップまでの実績と比べて消費メモリが増えそうなら、詳しく計算する。
			val oldMemoryUsage = program.getMemSize
			if (oldMemoryUsage < newMemoryUsage) {
				//増分に対する平方の少暇なを計算する。
				val memoryMana = ManaCost.calculateQuadraticMemoryMana(newMemoryUsage = newMemoryUsage, oldMemoryUsage = oldMemoryUsage)
				program.spendMana(memoryMana, op.name + " (memory usage)")
				manaCost += memoryMana
			}
			//コピー容量の課金。
			if (0 < copySize) {
				val copyMana = ManaCost.CopyMana * DataWord.countWords(copySize)
				manaCost += copyMana
				program.spendMana(copyMana, op.name + " (copy usage)")
			}
			//詳細デバッグ出力。
			if (program.getBlockNumber.longValue == NodeProperties.CONFIG.dumpBlock) {
				dumpLine(op, manaBefore, manaCost, newMemoryWords, program)
			}

			//処理を実行する。
			val hint = execute(op, program)

			//実行したコードを記録する。
			program.setPreviouslyExecutedOp(op.opcode)
			if (logger.isInfoEnabled && (op != Call) && (op != CallCode) && (op != Create)) {
				logger.info(logString.format("[%5s]".format(program.getPC), "%-12s".format(op.name), program.getMana.longValue, program.getCallDepth, hint))
			}
			vmCounter += 1

//			if ((vmCounter % 10) == 0) {
//				Thread.`yield`()
//			}
		} catch {
			case e: RuntimeException =>
				if (logger.isInfoEnabled) {
					logger.info("[VM] VM halted: [%s]", e.toString)
				}
				program.spendAllMana()
				program.resetFutureRefund()
				program.stop()
				throw e
		} finally {
			program.fullTrace()
		}
	}

	private def computeNecessaryResources(op: OpCode, program: Program): (Long, BigInt, Long) = {
		val stack = program.stack
		op match {
			case Stop | Suicide =>
				(ManaCost.Stop, Zero, 0L)
			case SStore =>
				val newValue = stack.get(-2)
				val oldValueOption = program.storageLoad(stack.peek)
				if (oldValueOption.isEmpty && !newValue.isZero) {
					(ManaCost.SetSStore, Zero, 0L)
				} else if (oldValueOption.nonEmpty && newValue.isZero) {
					program.futureRefundMana(ManaCost.RefundSStore)
					(ManaCost.ClearSStore, Zero, 0L)
				} else {
					(ManaCost.ResetSStore, Zero, 0L)
				}
			case SLoad =>
				(ManaCost.SLoad, Zero, 0L)
			case Balance =>
				(ManaCost.Balance, Zero, 0L)
			case MStore =>
				(op.tier.asInt, memNeeded(stack.peek, DataWord(DataWord.NUM_BYTES)), 0L)
			case MStore8 =>
				(op.tier.asInt, memNeeded(stack.peek, DataWord(1)), 0L)
			case MLoad =>
				(op.tier.asInt, memNeeded(stack.peek, DataWord(DataWord.NUM_BYTES)), 0L)
			case Return =>
				(ManaCost.Return, memNeeded(stack.peek, stack.get(-2)), 0L)
			case Keccak256 =>
				val size = stack.get(-2)
				val chunkUsed = DataWord.countWords(size.longValue)
				val manaCost = ManaCost.SHA3 + (chunkUsed * ManaCost.SHA3Word)
				(manaCost, memNeeded(stack.peek, size), 0L)
			case CallDataCopy =>
				val copySize = stack.get(-3).longValue
				(op.tier.asInt, memNeeded(stack.peek, stack.get(-3)), copySize)
			case CodeCopy =>
				val copySize = stack.get(-3).longValue
				(op.tier.asInt, memNeeded(stack.peek, stack.get(-3)), copySize)
			case ExtCodeCopy =>
				val copySize = stack.get(-4).longValue
				(op.tier.asInt, memNeeded(stack.get(-2), stack.get(-4)), copySize)
			case Call | CallCode =>
				val callManaWord = stack.get(-1)
				if (program.getMana.value < callManaWord.value) {
					throw Program.Exception.notEnoughOpMana(op, callManaWord, program.getMana)
				}
				var manaCost = ManaCost.Call + callManaWord.longValue
				val callAddressWord = stack.get(-2)

				if ((op != CallCode) && !program.storage.existsAccount(callAddressWord.last20Bytes)) {
					manaCost += ManaCost.NewAccountCall
				}
				if (!stack.get(-3).isZero) {
					manaCost += ManaCost.ValueTransferCall
				}
				val in = memNeeded(stack.get(-4), stack.get(-5))
				val out = memNeeded(stack.get(-6), stack.get(-7))
				(manaCost, in.max(out), 0L)
			case Create =>
				(ManaCost.Create, memNeeded(stack.get(-2), stack.get(-3)), 0L)
			case Log0 | Log1 | Log2 | Log3 | Log4 =>
				val numTopics = op.opcode - OpCode.Log0.opcode
				val dataSize = stack.get(-2).value
				val dataCost = dataSize * BigInt(ManaCost.LogDataMana)
				if (program.getMana.value < dataCost) {
					throw Program.Exception.notEnoughOpMana(op, dataCost, program.getMana.value)
				}
				val manaCost = ManaCost.LogMana + ManaCost.LogTopicMana * numTopics + ManaCost.LogDataMana * stack.get(-2).longValue
				(manaCost, memNeeded(stack.peek, stack.get(-2)), 0L)
			case Exp =>
				val exp = stack.get(-2)
				val bytesOccupied = exp.occupiedBytes
				(ManaCost.ExpMana + ManaCost.ExpByteMana * bytesOccupied, Zero, 0L)
			case _ => (op.tier.asInt, Zero, 0L)
		}
	}

	private def execute(op: OpCode, program: Program): String = {
		//処理を実行する。
		var hint = ""
		val stack = program.stack
		op match {
			case Stop =>
				program.setHReturn(ImmutableBytes.empty)
				program.stop()
			case Add =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d + %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 + word2)
				program.step()
			case Mul =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d * %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 * word2)
				program.step()
			case Sub =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d - %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 - word2)
				program.step()
			case Div =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d / %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 / word2)
				program.step()
			case SDiv =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d sdiv %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 sDiv word2)
				program.step()
			case Mod =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d mod %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 % word2)
				program.step()
			case SMod =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d smod %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 sMod word2)
				program.step()
			case Exp =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d exp %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 exp word2)
				program.step()
			case SignExtend =>
				val word1 = program.stackPop
				val k = word1.value
				if (k < WordLength) {
					val word2 = program.stackPop
					if (logger.isInfoEnabled) {
						hint = "%d  %d".format(word1.value, word2.value)
					}
					program.stackPush(word2.signExtend(k.byteValue()))
				}
				program.step()
			case Not =>
				val word = program.stackPop
				val result = ~word
				if (logger.isInfoEnabled) {
					hint = "%d".format(result.value)
				}
				program.stackPush(result)
				program.step()
			case LT =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d < %d".format(word1.value, word2.value)
				}
				val result =
					if (word1.value < word2.value) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case SLT =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d < %d".format(word1.sValue, word2.sValue)
				}
				val result =
					if (word1.sValue < word2.sValue) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case SGT =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d > %d".format(word1.sValue, word2.sValue)
				}
				val result =
					if (word1.sValue > word2.sValue) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case GT =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d > %d".format(word1.value, word2.value)
				}
				val result =
					if (word1.value > word2.value) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case Eq =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d == %d".format(word1.value, word2.value)
				}
				val result =
					if ((word1 ^ word2).isZero) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case IsZero =>
				val word = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d".format(word.value)
				}
				val result =
					if (word.isZero) {
						DataWord.One
					} else {
						DataWord.Zero
					}
				program.stackPush(result)
				program.step()
			case And =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d & %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 & word2)
				program.step()
			case Or =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d | %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 | word2)
				program.step()
			case Xor =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "%d xor %d".format(word1.value, word2.value)
				}
				program.stackPush(word1 ^ word2)
				program.step()
			case Byte =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				val result =
					if (word1.value < WordLength) {
						val temp: Byte = word2.data(word1.intValue)
						DataWord(temp & 0xFF)
					} else {
						DataWord.Zero
					}
				if (logger.isInfoEnabled) {
					hint = "%d".format(result.value)
				}
				program.stackPush(result)
				program.step()
			case AddMod =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				val word3 = program.stackPop
				val result = word1.addMod(word2, word3)
				program.stackPush(result)
				program.step()
			case MulMod =>
				val word1 = program.stackPop
				val word2 = program.stackPop
				val word3 = program.stackPop
				val result = word1.mulMod(word2, word3)
				program.stackPush(result)
				program.step()
			case Keccak256 =>
				val offset = program.stackPop
				val len = program.stackPop
				val buffer = program.memoryChunk(offset.intValue, len.intValue)
				//計算する。
				val result = DataWord(buffer.digest256.bytes)
				//TODO 未実装： StorageDictHandler
//				if (this.storageDictHandler ne null) {
//					storageDictHandler.vmSha3Notify(buffer, result)
//				}
				if (logger.isInfoEnabled) {
					hint = result.toString
				}
				program.stackPush(result)
				program.step()
			case Address =>
				val address = program.getOwnerAddress
				if (logger.isInfoEnabled) {
					hint = "address: " + address.last20Bytes.toHexString
				}
				program.stackPush(address)
				program.step()
			case Balance =>
				val address = program.stackPop
				val balance = program.getBalance(address)
				if (logger.isInfoEnabled) {
					hint = "address: %s; balance: %s".format(address.last20Bytes, balance)
				}
				program.stackPush(balance)
				program.step()
			case Origin =>
				val originAddress = program.getOriginAddress
				if (logger.isInfoEnabled) {
					hint = "address: " + originAddress.last20Bytes.toHexString
				}
				program.stackPush(originAddress)
				program.step()
			case Caller =>
				val callerAddress = program.getCallerAddress
				if (logger.isInfoEnabled) {
					hint = "address: " + callerAddress.last20Bytes.toHexString
				}
				program.stackPush(callerAddress)
				program.step()
			case CallValue =>
				val callValue = program.getCallValue
				if (logger.isInfoEnabled) {
					hint = "value: " + callValue
				}
				program.stackPush(callValue)
				program.step()
			case CallDataLoad =>
				val offset = program.stackPop
				val value = program.getDataValue(offset)
				if (logger.isInfoEnabled) {
					hint = "data: " + value
				}
				program.stackPush(value)
				program.step()
			case CallDataSize =>
				val dataSize = program.getDataSize
				if (logger.isInfoEnabled) {
					hint = "size: " + dataSize
				}
				program.stackPush(dataSize)
				program.step()
			case CallDataCopy =>
				//メッセージデータをメモリに格納する。
				val memOffset = program.stackPop
				val dataOffset = program.stackPop
				val length = program.stackPop
				val messageData = program.getDataCopy(dataOffset, length)
				if (logger.isInfoEnabled) {
					hint = "data: " + messageData
				}
				program.memorySave(memOffset, messageData, limited = false)
				program.step()
			case CodeSize | ExtCodeSize =>
				val length =
					if (op == CodeSize) {
						program.getCode.length
					} else {
						val address = program.stackPop
						program.getCodeAt(address).map(_.length).getOrElse(0)
					}
				val result = DataWord(length)
				if (logger.isInfoEnabled) {
					hint = "size: " + length
				}
				program.stackPush(result)
				program.step()
			case CodeCopy | ExtCodeCopy =>
				val fullCode =
					if (op == CodeCopy) {
						program.getCode
					} else {
						val address = program.stackPop
						program.getCodeAt(address).getOrElse(ImmutableBytes.empty)
					}
				val memOffset = program.stackPop
				val codeOffset = program.stackPop.intValueSafe
				val length = program.stackPop.intValueSafe
				val sizeToBeCopied =
					if (fullCode.length < (codeOffset + length)) {
						if (fullCode.length < codeOffset) {
							0
						} else {
							fullCode.length - codeOffset
						}
					} else {
						length
					}
				val result =
					if (codeOffset < fullCode.length) {
						fullCode.copyOfRange(codeOffset, codeOffset + sizeToBeCopied)
					} else {
						ImmutableBytes.create(length)
					}
				if (logger.isInfoEnabled) {
					hint = "code: " + result.toHexString
				}
				program.memorySave(memOffset, result, limited = false)
				program.step()
			case ManaPrice =>
				val manaPrice = program.getManaPrice
				if (logger.isInfoEnabled) {
					hint = "price: " + manaPrice
				}
				program.stackPush(manaPrice)
				program.step()
			case BlockHash =>
				val blockIndex = program.stackPop.intValue
				val blockHash = program.getBlockHash(blockIndex)
				if (logger.isInfoEnabled) {
					hint = "blockHash: " + blockHash
				}
				program.stackPush(blockHash)
				program.step()
			case Coinbase =>
				val coinbase = program.getCoinbase
				if (logger.isInfoEnabled) {
					hint = "Coinbase: " + coinbase.last20Bytes.toHexString
				}
				program.stackPush(coinbase)
				program.step()
			case Timestamp =>
				val timestamp = program.getTimestamp
				if (logger.isInfoEnabled) {
					hint = "timestamp: " + timestamp.value
				}
				program.stackPush(timestamp)
				program.step()
			case BlockNumber =>
				val number = program.getBlockNumber
				if (logger.isInfoEnabled) {
					hint = "number: " + number.value
				}
				program.stackPush(number)
				program.step()
			case Difficulty =>
				val difficulty = program.getDifficulty
				if (logger.isInfoEnabled) {
					hint = "difficulty: " + difficulty.value
				}
				program.stackPush(difficulty)
				program.step()
			case ManaLimit =>
				val manaLimit = program.getBlockManaLimit
				if (logger.isInfoEnabled) {
					hint = "manaLimit: " + manaLimit.value
				}
				program.stackPush(manaLimit)
				program.step()
			case Pop =>
				program.stackPop
				program.step()
			case Dup1 | Dup2 | Dup3 | Dup4 | Dup5 | Dup6 | Dup7 | Dup8 | Dup9 | Dup10 | Dup11 | Dup12 | Dup13 | Dup14 | Dup15 | Dup16 =>
				val n = op.opcode - OpCode.Dup1.opcode + 1
				val word = stack.get(stack.size - n)
				program.stackPush(word)
				program.step()
			case Swap1 | Swap2 | Swap3 | Swap4 | Swap5 | Swap6 | Swap7 | Swap8 | Swap9 | Swap10 | Swap11 | Swap12 | Swap13 | Swap14 | Swap15 | Swap16 =>
				val n = op.opcode - OpCode.Swap1.opcode + 2
				stack.swap(stack.size - 1, stack.size - n)
				program.step()
			case Log0 | Log1 | Log2 | Log3 | Log4 =>
				val address = program.getOwnerAddress
				val memStart = stack.pop
				val memSize = stack.pop
				val nTopics = op.opcode - OpCode.Log0.opcode
				val topics = new ArrayBuffer[DataWord](nTopics)
				(0 until nTopics).foreach {
					_ => topics.append(stack.pop)
				}
				val data = program.memoryChunk(memStart.intValue, memSize.intValue)
				val logInfo = new LogInfo(address.last20Bytes, topics.toSeq, data)
				if (logger.isInfoEnabled) {
					hint = logInfo.toString
				}
				program.result.addLog(logInfo)
				program.step()
			case MLoad =>
				val address = program.stackPop
				val data = program.memoryLoad(address)
				if (logger.isInfoEnabled) {
					hint = "data: " + data
				}
				program.stackPush(data)
				program.step()
			case MStore =>
				val address = program.stackPop
				val value = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "addr: " + address + " value: " + value
				}
				program.memorySave(address, value)
				program.step()
			case MStore8 =>
				val address = program.stackPop
				val value = program.stackPop
				val byteValue = value.data.last
				program.memorySave(address, ImmutableBytes.fromOneByte(byteValue), limited = false)
				program.step()
			case SLoad =>
				val key = program.stackPop
				val value = program.storageLoad(key).getOrElse(DataWord.Zero)
				if (logger.isInfoEnabled) {
					hint = "key: " + key + " value: " + value
				}
				program.stackPush(value)
				program.step()
			case SStore =>
				val key = program.stackPop
				val value = program.stackPop
				if (logger.isInfoEnabled) {
					hint = "[%s] key: %s value: %s".format(program.getOwnerAddress.toPrefixString, key, value)
				}
				program.storageSave(key, value)
				//TODO 未実装：StorageDictHandler
//				if (storageDictHandler ne null) {
//					storageDictHandler.vmSStoreNotify(addr, value)
//				}
				program.step()
			case Jump =>
				val pos = program.stackPop
				program.verifyJumpDest(pos) match {
					case Right(nextPC) =>
						if (logger.isInfoEnabled) {
							hint = "~> " + nextPC
						}
						program.setPC(nextPC)
					case Left(e) => throw e
				}
			case JumpI =>
				val pos = program.stackPop
				val cond = program.stackPop
				if (!cond.isZero) {
					program.verifyJumpDest(pos) match {
						case Right(nextPC) =>
							if (logger.isInfoEnabled) {
								hint = "~> " + nextPC
							}
							program.setPC(nextPC)
						case Left(e) => throw e
					}
				} else {
					program.step()
				}
			case PC =>
				val pc = DataWord(program.getPC)
				if (logger.isInfoEnabled) {
					hint = pc.intValue.toString
				}
				program.stackPush(pc)
				program.step()
			case MSize =>
				val memSize = DataWord(program.getMemSize)
				if (logger.isInfoEnabled) {
					hint = memSize.intValue.toString
				}
				program.stackPush(memSize)
				program.step()
			case Mana =>
				val mana = program.getMana
				if (logger.isInfoEnabled) {
					hint = mana.intValue.toString
				}
				program.stackPush(mana)
				program.step()
			case Push1 | Push2 | Push3 | Push4 | Push5 | Push6 | Push7 | Push8 | Push9 | Push10 | Push11 | Push12 | Push13 | Push14 | Push15 | Push16 |
			     Push17 | Push18 | Push19 | Push20 | Push21 | Push22 | Push23 | Push24 | Push25 | Push26 | Push27 | Push28 | Push29 | Push30 | Push31 | Push32 =>
				program.step()
				val nPush = op.opcode - Push1.opcode + 1
				val data = program.sweep(nPush)
				if (logger.isInfoEnabled) {
					hint = data.toHexString
				}
				program.stackPush(DataWord(data))
			case JumpDest =>
				program.step()
			case Create =>
				val value = program.stackPop
				val inOffset = program.stackPop
				val inSize = program.stackPop
				if (logger.isInfoEnabled) {
					logger.info(logString.format("[%5s]".format(program.getPC), "%-12s".format(op.name), program.getMana.value, program.getCallDepth, hint))
				}
				program.createContract(value, inOffset, inSize)
				program.step()
			case Call | CallCode =>
				var mana = program.stackPop
				val codeAddress = program.stackPop
				val value = program.stackPop
				if (!value.isZero) {
					mana += DataWord(ManaCost.StipendCall)
				}
				val inDataOffset = program.stackPop
				val inDataSize = program.stackPop
				val outDataOffset = program.stackPop
				val outDataSize = program.stackPop

				if (logger.isInfoEnabled) {
					hint = "addr: %s gas: %s inOffset: %s inSize: %s".format(codeAddress.last20Bytes.toHexString, mana.shortHex, inDataOffset.shortHex, inDataSize.shortHex)
					logger.info(logString.format("[%5s]".format(program.getPC), "%-12s".format(op.name), program.getMana.value, program.getCallDepth, hint))
				}

				program.memoryExpand(outDataOffset, outDataSize)
				val message = MessageCall(
					if (op == Call) MessageType.Call else MessageType.Stateless,
					mana, codeAddress, value, inDataOffset, inDataSize, outDataOffset, outDataSize
				)
				PrecompiledContracts.getContractForAddress(codeAddress) match {
					case Some(contract) =>
						program.invokePrecompiledContractCode(message, contract)
					case _ =>
						program.invokeContractCode(message)
				}
				program.step()
			case Return =>
				val offset = program.stackPop
				val size = program.stackPop
				val hReturn = program.memoryChunk(offset.intValue, size.intValue)
				program.setHReturn(hReturn)
				if (logger.isInfoEnabled) {
					hint = "data: " + hReturn.toHexString
				}
				program.step()
				program.stop()
			case Suicide =>
				val address = program.stackPop
				program.suicide(address)
				if (logger.isInfoEnabled) {
					hint = "address: " + program.getOwnerAddress.last20Bytes.toHexString
				}
				program.stop()
			case _ =>
			//
		}
		hint
	}

	/**
	 * 命令実行に必要となる、新たな総メモリ容量を計算して返します。
	 */
	private def memNeeded(offset: DataWord, size: DataWord): BigInt = {
		if (size.isZero) Zero else offset.value + size.value
	}

	private def dumpLine(op: OpCode, manaBefore: Long, manaCost: Long, memWords: Long, program: Program): Unit = {
//		op match {
//			case Stop | Return | Suicide =>
//				val details = program.storage.getContractDetails(program.getOwnerAddress.last20Bytes)
//				val storageKeys = details.get new ArrayList[DataWord](details.getStorage.keySet)
//				Collections.sort(storageKeys)
//				import scala.collection.JavaConversions._
//				for (key <- storageKeys) {
//					dumpLogger.trace("{} {}", Hex.toHexString(key.getNoLeadZeroesData), Hex.toHexString(details.getStorage.get(key).getNoLeadZeroesData))
//				}
//			case _ =>
//				break
//		}
		val addressString = program.getOwnerAddress.last20Bytes.toHexString
		val pcString = program.getPC.toString
		val opString = "%d (%s)".format(op.opcode, op.name)
		val manaString = program.getMana.getDataWithoutLeadingZeros.toHexString

		dumpLogger.trace("{} {} {} {}", addressString, pcString, opString, manaString)
	}

}

object VM {
	private val logger = LoggerFactory.getLogger("vm")
	private val dumpLogger = LoggerFactory.getLogger("dump")

	private val Zero = UtilConsts.Zero
	private val WordLength = BigInt(DataWord.NUM_BYTES)
	private val logString = "[%s]    Op: [%s]  Mana: [%s] Deep: [%,d]  Hint: [%s]"

	private val MaxMana = BigInt(Long.MaxValue)

}