package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.OpCode._
import org.lipicalabs.lipica.core.vm.program.Program
import org.slf4j.LoggerFactory

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

	def step(program: Program): Unit = {
		if (SystemProperties.CONFIG.vmTrace) {
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

			var hint = ""

			val manaBefore = program.getMana.longValue
			val stepBefore: Int = program.getPC

			//必要なマナ容量および新たに必要となるメモリサイズを計算する。
			val (baseManaCost, newMemSize, copySize) = computeNecessaryResources(op, program)
			//マナを消費する。
			program.spendMana(baseManaCost, op.toString)

			if (MaxMana < newMemSize) {
				throw Program.Exception.manaOverflow(newMemSize, MaxMana)
			}

			//マナコストを、さらに詳しく計算する。
			var manaCost = baseManaCost
			val oldMemSize = program.getMemSize
			val memoryUsage = (newMemSize.longValue() + DataWord.NUM_BYTES - 1) / DataWord.NUM_BYTES * DataWord.NUM_BYTES
			var memWords: Long = 0
			if (oldMemSize < memoryUsage) {
				memWords = memoryUsage / DataWord.NUM_BYTES
				val memWordsOld = oldMemSize / DataWord.NUM_BYTES
				val memMana = (ManaCost.Memory * memWords + memWords * memWords / 512) - (ManaCost.Memory * memWordsOld + memWordsOld * memWordsOld / 512)
				program.spendMana(memMana, op.name + " (memory usage)")
				manaCost += memMana
			}
			if (0 < copySize) {
				val copyMana = ManaCost.CopyMana * ((copySize + DataWord.NUM_BYTES - 1) / DataWord.NUM_BYTES)
				manaCost += copyMana
				program.spendMana(copyMana, op.name + " (copy usage)")
			}
			//詳細デバッグ出力。
			if (program.getBlockNumber.longValue == SystemProperties.CONFIG.dumpBlock) {
				dumpLine(op, manaBefore, manaCost, memWords, program)
			}

			//処理を実行する。
			execute(op, program)

			//実行したコードを記録する。
			program.setPreviouslyExecutedOp(op.opcode)
			if (logger.isInfoEnabled && (op != Call) && (op != CallCode) && (op != Create)) {
				logger.info(logString.format("[%5s]".format(program.getPC), "%-12s".format(op.name), program.getMana.longValue, program.getCallDepth, hint))
			}
			vmCounter += 1
		} catch {
			case e: RuntimeException =>
				logger.warn("VM halted: [%s]", e.toString)
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
				val oldValue = program.storageLoad(stack.peek)
				if ((oldValue eq null) && !newValue.isZero) {
					(ManaCost.SetSStore, Zero, 0L)
				} else if ((oldValue eq null) && newValue.isZero) {
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
			case SHA3 =>
				val size = stack.get(-2)
				val chunkUsed = (size.longValue + DataWord.NUM_BYTES - 1) / DataWord.NUM_BYTES
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
			case Lt =>
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
			case SLt =>
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
			case SGt =>
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
			case Gt =>
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

	def play(program: Program): Unit = {
		try {
			if (SystemProperties.CONFIG.isStorageDictionaryEnabled) {
				//TODO
				//storageDictHandler = new StorageDictionaryHandler(program.getOwnerAddress)
				//storageDictHandler.vmStartPlayNotify
			}
			if (program.byTestingSuite) return
			while (!program.isStopped) {
				this.step(program)
			}
			//TODO
//			if (storageDictHandler != null) {
//				val details = program.storage.getContractDetails(program.getOwnerAddress.last20Bytes)
//				storageDictHandler.vmEndPlayNotify(details)
//			}
		} catch {
			case e: RuntimeException =>
				program.setRuntimeFailure(e)
			case stackOverFlowError: StackOverflowError =>
				logger.error("\n !!! StackOverflowError: update your java run command with -Xss32M !!!\n")
				System.exit(-1)
		}
	}

	private def dumpLine(op: OpCode, manaBefore: Long, manaCost: Long, memWords: Long, program: Program): Unit = {
		//TODO 未実装：dumpLine
	}

}

object VM {
	private val logger = LoggerFactory.getLogger("VM")
	private val dumpLogger = LoggerFactory.getLogger("dump")

	private val Zero = BigInt(0)
	private val WordLength = BigInt(DataWord.NUM_BYTES)
	private val logString = "[%s]    Op: [%s]  Mana: [%s] Deep: [%,d]  Hint: [%s]"

	private val MaxMana = BigInt(Long.MaxValue)

}