package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.base.{Repository, TransactionLike}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.PrecompiledContracts.PrecompiledContract
import org.lipicalabs.lipica.core.vm.trace.{ProgramTrace, ProgramTraceListener}
import org.lipicalabs.lipica.core.vm.{ManaCost, VM, DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.invoke.{ProgramInvokeFactory, ProgramInvokeFactoryImpl, ProgramInvoke}
import org.lipicalabs.lipica.core.vm.program.listener.{CompositeProgramListener, ProgramListenerAware}
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Program(private val ops: ImmutableBytes, private val invoke: ProgramInvoke, private val transaction: TransactionLike) {

	import Program._

	/** 各種リスナ。 */
	private var listener: ProgramOutListener = null
	private val traceListener = new ProgramTraceListener
	private val programListener = CompositeProgramListener(Some(this.traceListener))

	/** プログラムカウンタ。 */
	private var pc = 0

	private val memory = setupProgramListener(new Memory)
	val stack = setupProgramListener(new Stack)
	val storage = setupProgramListener(Storage(invoke))

	val result = new ProgramResult
	val trace = new ProgramTrace(invoke)

	private val programInvokeFactory: ProgramInvokeFactory = new ProgramInvokeFactoryImpl

	/**
	 * JUMPDEST命令がある箇所の索引。
	 */
	private var jumpDests: Set[Int] = Set.empty

	private var stopped = false

	/** 最終オペコード。 */
	private var lastOp = 0
	/** 前回実行されたオペコード。 */
	private var previouslyExecutedOp = 0


	precompile()

	private def setupProgramListener[T <: ProgramListenerAware](traceListenerAware: T): T = {
		traceListenerAware.setTraceListener(this.traceListener)
		traceListenerAware
	}

	def getCallDeep: Int = this.invoke.getCallDeep

	private def addInternalTx(nonce: ImmutableBytes, manaLimit: DataWord, senderAddress: ImmutableBytes, receiveAddress: ImmutableBytes, value: BigInt, data: ImmutableBytes, note: String): InternalTransaction = {
		if (this.transaction ne null) {
			val senderNonce =
				if (nonce.isEmpty) {
					ImmutableBytes.asSignedByteArray(this.storage.getNonce(senderAddress))
				} else {
					nonce
				}
			this.result.addInternalTransaction(this.transaction.hash, getCallDeep, senderNonce, getManaPrice, manaLimit, senderAddress, receiveAddress, ImmutableBytes.asSignedByteArray(value), data, note)
		} else {
			null
		}
	}

	def getCurrentOp: Byte = {
		if (ByteUtils.isNullOrEmpty(this.ops)) {
			0
		} else {
			this.ops(this.pc)
		}
	}

	/** ログ出力用に、最終オペコードを保持させます。 */
	def setLastOp(op: Byte): Unit = {
		this.lastOp = op
	}

	def setPreviouslyExecutedOp(op: Byte): Unit = {
		this.previouslyExecutedOp = op
	}

	/* スタックへのpush操作。 */
	def stackPush(word: DataWord): Unit = {
		verifyStackOverflow(0, 1)
		this.stack.push(word)
	}
	def stackPush(data: Array[Byte]): Unit = stackPush(DataWord(data))
	def stackPushZero(): Unit = stackPush(DataWord.Zero)
	def stackPushOne(): Unit = stackPush(DataWord.One)

	def verifyStackSize(requiredSize: Int): Unit = {
		if (this.stack.size < requiredSize) {
			throw Exception.tooSmallStack(requiredSize, this.stack.size)
		}
	}
	def verifyStackOverflow(argsReqs: Int, returnReqs: Int): Unit = {
		val requiredSize = this.stack.size - argsReqs + returnReqs
		if (MaxStackSize < requiredSize) {
			throw Exception.stackOverflow(MaxStackSize, requiredSize)
		}
	}

	/**
	 * スタックから値をpopして返します。
	 */
	def stackPop: DataWord = this.stack.pop

	/** プログラムカウンタへの操作。 */
	def getPC: Int = this.pc
	def setPC(v: Int): Unit = {
		this.pc = v

		if (this.ops.length <= this.pc) {
			stop()
		}
	}
	def setPC(v: DataWord): Unit = setPC(v.intValue)

	/**
	 * 実行を１段階進めます。
	 */
	def step(): Unit = {
		setPC(this.pc + 1)
	}

	/**
	 * 現在箇所からN個のオペコードを読み取って返すと同時に、
	 * プログラムカウンタをN個進めます。
	 */
	def sweep(n: Int): ImmutableBytes = {
		if (this.ops.length <= this.pc + n) {
			stop()
		}
		val result = this.ops.copyOfRange(this.pc, this.pc + n)
		this.pc += n
		result
	}

	def isStopped: Boolean = this.stopped
	def stop(): Unit = {
		this.stopped = true
	}

	def setHReturn(v: ImmutableBytes): Unit = {
		this.result.setHReturn(v)
	}

	/** メモリ操作 */
	def getMemSize: Int = this.memory.size

	def memorySave(addr: Int, value: ImmutableBytes, len: Int, limited: Boolean): Unit = {
		this.memory.write(addr, value, len, limited)
	}
	def memorySave(addr: DataWord, value: ImmutableBytes, limited: Boolean): Unit = {
		this.memory.write(addr.intValue, value, value.length, limited)
	}
	private def memorySave(addr: DataWord, value: DataWord, limited: Boolean): Unit = {
		memorySave(addr, value.data, limited)
	}
	def memorySave(addr: DataWord, value: DataWord): Unit = memorySave(addr, value, limited = false)
	def memorySaveLimited(addr: DataWord, value: DataWord): Unit = memorySave(addr, value, limited = true)
	def memorySave(addr: Int, allocSize: Int, value: ImmutableBytes): Unit = {
		this.memory.extendAndWrite(addr, allocSize, value)
	}
	def memoryLoad(addr: DataWord): DataWord = {
		this.memory.readWord(addr.intValue)
	}
	def memoryChunk(offset: Int, size: Int): ImmutableBytes = {
		this.memory.read(offset, size)
	}
	def memoryExpand(outDataOffset: DataWord, outDataSize: DataWord): Unit = {
		allocateMemory(outDataOffset.intValue, outDataSize.intValue)
	}
	def allocateMemory(offset: Int, size: Int): Unit = {
		if (0 < size) {
			this.memory.extend(offset, size)
		}
	}
	/** テスト用に、メモリの中身をコピーして返します。 */
	private[vm] def getMemoryContent: ImmutableBytes = {
		this.memory.read(0, memory.size)
	}
	/** テスト用に、メモリの中身を渡された内容に書き換えます。 */
	private[vm] def initMemory(data: ImmutableBytes): Unit = {
		this.memory.write(0, data, data.length, limited = false)
	}

	/** ストレージ操作。 */
	def storageSave(key: DataWord, value: DataWord): Unit = {
		this.storage.addStorageRow(getOwnerAddress.last20Bytes, key, value)
	}
	def storageSave(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		storageSave(DataWord(key), DataWord(value))
	}
	def storageLoad(key: DataWord): DataWord = {
		this.storage.getStorageValue(getOwnerAddress.last20Bytes, key)
	}

	def suicide(obtainerAddress: DataWord): Unit = {
		val owner = getOwnerAddress.last20Bytes
		val obtainer = obtainerAddress.last20Bytes
		val balance = this.storage.getBalance(owner)
		if (logger.isInfoEnabled) {
			logger.info("Transfer to [%s] heritage: [%s]".format(obtainer.toHexString, balance))
		}

		addInternalTx(ImmutableBytes.empty, DataWord.Zero, owner, obtainer, balance, ImmutableBytes.empty, "suicide")
		Repository.transfer(this.storage, owner, obtainer, balance)
		result.addDeleteAccount(getOwnerAddress)
	}

	def createContract(value: DataWord, memStart: DataWord, memSize: DataWord): Unit = {
		if (getCallDeep == MaxDepth) {
			stackPushZero()
			return
		}

		val senderAddress = this.getOwnerAddress.last20Bytes
		val endowment = value.value
		if (this.storage.getBalance(senderAddress) < endowment) {
			stackPushZero()
			return
		}
		//メモリからコードを読み取る。
		val programCode = memoryChunk(memStart.intValue, memSize.intValue)
		if (logger.isInfoEnabled) {
			logger.info("Creating a new contract inside contract run: [%s]".format(senderAddress.toHexString))
		}
		//マナを消費する。
		val manaLimit = getMana
		spendMana(manaLimit.longValue, "internal call")

		//コントラクト用アドレスを生成する。
		val nonce = ImmutableBytes.asSignedByteArray(this.storage.getNonce(senderAddress))
		val newAddress = DigestUtils.computeNewAddress(getOwnerAddress.last20Bytes, nonce)

		if (byTestingSuite) {
			this.result.addCallCreate(programCode, ImmutableBytes.empty, manaLimit.getDataWithoutLeadingZeros, value.getDataWithoutLeadingZeros)
		}
		//nonceを更新する。
		if (!byTestingSuite) {
			this.storage.increaseNonce(senderAddress)
		}

		val track = this.storage.startTracking
		//ハッシュ値衝突が発生した場合の配慮のため、残高を検査する。
		if (track.existsAccount(newAddress)) {
			val oldBalance = track.getBalance(newAddress)
			track.createAccount(newAddress)
			track.addBalance(newAddress, oldBalance)
		} else {
			track.createAccount(newAddress)
		}
		//移動を実行する。
		track.addBalance(senderAddress, -endowment)
		val newBalance: BigInt =
			if (!byTestingSuite) {
				track.addBalance(newAddress, endowment)
			} else {
				BigInt(0)
			}
		//実行する。
		val internalTx = addInternalTx(nonce, getManaLimit, senderAddress, ImmutableBytes.empty, endowment, programCode, "create")
		//TODO 未実装！！！
		val programInvoke: ProgramInvoke = this.programInvokeFactory.createProgramInvoke(this, DataWord(newAddress), DataWord.Zero, manaLimit, newBalance, ImmutableBytes.empty, track, this.invoke.getBlockStore, byTestingSuite)

		val programResult =
			if (programCode.nonEmpty) {
				val vm = new VM
				val program = new Program(programCode, programInvoke, internalTx)
				vm.play(program)
				val localResult = program.result
				this.result.addInternalTransactions(result.getInternalTransactions)

				if (localResult.exception ne null) {
					//エラーが発生した。
					if (logger.isDebugEnabled) {
						logger.debug("contract run halted by Exception: contract: [%s], exception: [%s]".format(newAddress.toHexString, localResult.exception))
					}
					internalTx.reject()
					localResult.rejectInternalTransactions()

					track.rollback()
					stackPushZero()
					return
				}
				localResult
			} else {
				ProgramResult.createEmpty
			}

		//コントラクトを保存する。
		val contractCode = programResult.getHReturn
		val storageCost = contractCode.length * ManaCost.CreateData
		val afterSpend = programInvoke.getMana.longValue - storageCost - programResult.manaUsed
		if (afterSpend < 0L) {
			//残金不足。
			track.saveCode(newAddress, ImmutableBytes.empty)
		} else {
			//料金を消費し、コントラクトのコードを保存する。
			programResult.spendMana(storageCost)
			track.saveCode(newAddress, contractCode)
		}

		track.commit()
		this.result.addDeleteAccounts(programResult.getDeleteAccounts)
		this.result.addLogInfos(programResult.getLogInfoList)

		//生成されたアドレスを、スタックにプッシュする。
		stackPush(DataWord(newAddress))

		//残金をリファンドする。
		val refundMana = manaLimit.longValue - programResult.manaUsed
		if (0 < refundMana) {
			this.refundMana(refundMana, "Remaining mana from the internal call.")
			if (manaLogger.isInfoEnabled) {
				manaLogger.info("The remaining mana is refunded, account[%s], mana: [%s]".format(getOwnerAddress.last20Bytes.toHexString, refundMana))
			}
		}
	}

	/**
	 * 「コード」呼び出しを実行します。
	 *
	 * 通常のコールは、自身の状態を更新するコントラクトを呼び出します。
	 * ステートレスコールは、別のコントラクトに属するコールを、呼び出し元の文脈において呼び出します。
	 */
	def callToAddress(message: MessageCall): Unit = {
		if (getCallDeep == MaxDepth) {
			//スタックオーバーフロー。
			stackPushZero()
			refundMana(message.mana.longValue, "Call deep limit reached.")
			return
		}
		//入力データをメモリから読み取る。
		val data = memoryChunk(message.inDataOffset.intValue, message.inDataSize.intValue)

		val senderAddress = this.getOwnerAddress.last20Bytes
		val codeAddress = message.codeAddress.last20Bytes
		val contextAddress =
			if (message.msgType == MessageType.Stateless) {
				senderAddress
			} else {
				codeAddress
			}
		if (logger.isInfoEnabled) {
			logger.info("%s for existing contract: address: [%s], outDataOffset: [%s], outDataSize[%s]".format(contextAddress.toHexString, message.outDataOffset, message.outDataSize))
		}
		val track = this.storage.startTracking
		//手数料。
		val endowment = message.endowment.value
		val senderBalance = track.getBalance(senderAddress)
		if (senderBalance < endowment) {
			//手数料を払えない。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Refund mana from message call.")
			return
		}
		//コードを取得する。
		val programCode =
			if (this.storage.existsAccount(codeAddress)) {
				this.storage.getCode(codeAddress)
			} else {
				ImmutableBytes.empty
			}
		track.addBalance(senderAddress, -endowment)

		val contextBalance =
			if (byTestingSuite) {
				this.result.addCallCreate(data, contextAddress, message.mana.getDataWithoutLeadingZeros, message.endowment.getDataWithoutLeadingZeros)
				BigInt(0)
			} else {
				track.addBalance(contextAddress, endowment)
			}

		//内部トランザクションを生成する。
		val internalTx = addInternalTx(ImmutableBytes.empty, getManaLimit, senderAddress, contextAddress, endowment, programCode, "call")

		val programResultOption =
			if (programCode.nonEmpty) {
				val programInvoke: ProgramInvoke = this.programInvokeFactory.createProgramInvoke(this, DataWord(contextAddress), message.endowment, message.mana, contextBalance, data, track, this.invoke.getBlockStore, byTestingSuite)

				val vm = new VM
				val program = new Program(programCode, programInvoke, internalTx)
				vm.play(program)
				val localResult = program.result

				this.trace.mergeToThis(program.trace)
				this.result.mergeToThis(localResult)

				if (localResult.exception ne null) {
					//エラーが発生した。
					if (logger.isDebugEnabled) {
						logger.debug("contract run halted by Exception: contract: [%s], exception: [%s]".format(contextAddress.toHexString, localResult.exception))
					}
					internalTx.reject()
					localResult.rejectInternalTransactions()

					track.rollback()
					stackPushZero()
					return
				}
				Option(localResult)
			} else {
				None
			}
		//実行結果をメモリに書き込む。
		if (programResultOption.isDefined) {
			val hReturn = programResultOption.get.getHReturn
			memorySave(message.outDataOffset.intValue, hReturn, message.outDataSize.intValue, limited = true)
		}
		//成功を確定させる。
		track.commit()
		stackPushOne()

		//残りのマナをリファンドする。
		if (programResultOption.isDefined) {
			//利用されなかった残額を計算する。
			val refundMana = message.mana.value - BigInt(programResultOption.get.manaUsed)
			if (0 < refundMana.signum) {
				this.refundMana(refundMana.longValue(), "Remaining mana from the internal call.")
				if (manaLogger.isInfoEnabled) {
					manaLogger.info("The remaining mana refunded, account: [%s], mana: [%s]".format(senderAddress.toHexString, refundMana))
				}
			}
		} else {
			//全額。
			this.refundMana(message.mana.longValue, "remaining gas from the internal call")
		}
	}

	/**
	 * 実装済みのコントラクトを実行します。
	 */
	def callToPrecompiledAddress(message: MessageCall, contract: PrecompiledContract): Unit = {
		if (getCallDeep == MaxDepth) {
			//スタックの深さが限界。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Call deep limit reached.")
			return
		}

		val track = this.storage.startTracking
		val senderAddress = this.getOwnerAddress.last20Bytes
		val codeAddress = message.codeAddress.last20Bytes
		val contextAddress =
			if (message.msgType == MessageType.Stateless) {
				senderAddress
			} else {
				codeAddress
			}

		//手数料。
		val endowment = message.endowment.value
		val senderBalance = track.getBalance(senderAddress)
		if (senderBalance < endowment) {
			//手数料を払えない。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Refund mana from message call.")
			return
		}

		val data = this.memoryChunk(message.inDataOffset.intValue, message.inDataSize.intValue)
		//手数料を取る。
		Repository.transfer(track, senderAddress, contextAddress, message.endowment.value)

		if (byTestingSuite) {
			//テストなので、生成されたコールを蓄積する。
			this.result.addCallCreate(data, message.codeAddress.last20Bytes, message.mana.getDataWithoutLeadingZeros, message.endowment.getDataWithoutLeadingZeros)
			stackPushOne()
			return
		}
		//データに応じたコストを計算する。
		val requiredMana = contract.manaForData(data)
		if (message.mana.longValue < requiredMana) {
			//支払えない。
			this.refundMana(0, "Call pre-compiled.")
			this.stackPushZero()
			track.rollback()
		} else {
			//残額を返金する。
			this.refundMana(message.mana.longValue - requiredMana, "call pre-compiled")
			//実行する。
			val out = contract.execute(data)
			this.memorySave(message.outDataOffset, out, limited = false)
			this.stackPushOne()
			track.commit()
		}
	}

	def spendMana(manaValue: Long, cause: String): Unit = {
		manaLogger.info("[%s] Spent for cause: [%s], mana: [%,d]".format(invoke.hashCode, cause, manaValue))
		if ((getMana.longValue - manaValue) < 0) {
			throw Exception.notEnoughSpendingMana(cause, manaValue, this)
		}
		this.result.spendMana(manaValue)
	}
	def spendAllMana(): Unit = {
		spendMana(getMana.longValue, "Spending all remaining")
	}
	def refundMana(manaValue: Long, cause: String): Unit = {
		manaLogger.info("[%s] Refund for cause: [%s], mana: [%,d]".format(invoke.hashCode, cause, manaValue))
		this.result.refundMana(manaValue)
	}
	def futureRefundMana(manaValue: Long): Unit = {
		logger.info("Future refund added: %,d".format(manaValue))
		this.result.addFutureRefund(manaValue)
	}
	def resetFutureRefund(): Unit = this.result.resetFutureRefund()

	def getCode: ImmutableBytes = this.ops

	/**
	 * あるアドレスに結び付けられたコードをロードして返します。
	 */
	def getCodeAt(address: DataWord): ImmutableBytes = {
		this.invoke.getRepository.getCode(address.last20Bytes)
	}

	def getOwnerAddress: DataWord = this.invoke.getOwnerAddress

	def getBlockHash(index: Int): DataWord = {
		if ((index < this.getNumber.longValue) && (256.max(this.getNumber.intValue) - 256 <= index)) {
			//最近256ブロック内である。
			val loaded = this.invoke.getBlockStore.getBlockHashByNumber(index, getPrevHash.data)
			DataWord(loaded)
		} else {
			//古すぎるか未知。
			DataWord.Zero
		}
	}

	def getBalance(address: DataWord): DataWord = {
		val balance = this.storage.getBalance(address.last20Bytes)
		DataWord(ImmutableBytes.asSignedByteArray(balance))
	}

	def getOriginAddress = this.invoke.getOriginalAddress
	def getCallerAddress = this.invoke.getCallerAddress
	def getManaPrice = this.invoke.getMinManaPrice
	def getMana = {
		val remaining = invoke.getMana.longValue - this.result.manaUsed
		DataWord(remaining)
	}
	def getCallValue = this.invoke.getCallValue
	def getDataSize = this.invoke.getDataSize
	def getDataValue(index: DataWord) = this.invoke.getDataValue(index)
	def getDataCopy(offset: DataWord, length: DataWord): Array[Byte] = this.invoke.getDataCopy(offset, length)
	def getPrevHash = this.invoke.getPrevHash
	def getCoinbase = this.invoke.getCoinbase
	def getTimestamp = this.invoke.getTimestamp
	def getNumber = this.invoke.getNumber
	def getDifficulty = this.invoke.getDifficulty
	def getManaLimit = this.invoke.getManaLimit

	def setRuntimeFailure(e: RuntimeException): Unit = {
		this.result.exception = e
	}

	def memoryToString: String = this.memory.toString

	//TODO fullTrace
	//TODO stringify multiline
	//TODO stringify(not used)

	/**
	 * 現在のプログラムカウンタにおける文脈情報を、
	 * トレース領域に保存します。
	 */
	def saveOpTrace(): Unit = {
		if (this.pc < this.ops.length) {
			this.trace.addOp(this.ops(this.pc), this.pc, getCallDeep, getMana, this.traceListener.resetActions())
		}
	}

	private def precompile(): Unit = {
		var i = 0
		while (i < this.ops.length) {
			//オペコードを走査する。
			OpCode.code(this.ops(i)).foreach {eachOp => {
				if (eachOp == OpCode.JumpDest) {
					//この箇所を記憶しておく。
					this.jumpDests = this.jumpDests + i
				}
				val code = eachOp.opcode.toInt
				if ((OpCode.Push1.opcode.toInt <= code) && (code <= OpCode.Push32.opcode.toInt)) {
					//Push対象データは、読み飛ばす。
					i += ((code - OpCode.Push1.opcode.toInt) + 1)
				}
				i += 1
			}}
		}
	}

	/**
	 * 渡されたプログラムカウンタによって参照されるバイトコードが、
	 * JUMPDEST命令であることを確認します。
	 */
	def verifyJumpDest(nextPC: DataWord): Either[RuntimeException, Int] = {
		if (4 < nextPC.occupiedBytes) {
			Left(Exception.badJumpDestination(-1))
		}
		//次のプログラムカウンタ（＝添字）によって引かれるバイトコードが
		//JUMPDEST命令であることを確認する。
		val next = nextPC.intValue
		if (!this.jumpDests.contains(next)) {
			Left(Exception.badJumpDestination(next))
		} else {
			Right(next)
		}
	}

	def setListener(programOutListener: ProgramOutListener): Unit = {
		this.listener = programOutListener
	}

	def byTestingSuite: Boolean = this.invoke.byTestingSuite

}

object Program {
	private val logger = LoggerFactory.getLogger("VM")
	private val manaLogger = LoggerFactory.getLogger("mana")

	/**
	 * LVMの関数呼び出しの限界となる深さ。
	 * JVMには -Xss10M 程度が必要になる。
	 */
	private val MaxDepth = 1024

	private val MaxStackSize = 1024

	/**
	 * スタックが予想されたほど深くない場合に発生する例外。
	 */
	class StackTooSmallException(message: String, args: Any*) extends RuntimeException(message.format(args)) {
		//
	}

	/**
	 * スタックオーバーフロー時に発生する例外。
	 */
	class StackTooLargeException(message: String, args: Any*) extends RuntimeException(message.format(args)) {
		//
	}

	/**
	 * JUMPもしくはJUMPI命令のジャンプ先が正当でない場合に発生する例外。
	 */
	class BadJumpDestinationException(message: String, args: Any*) extends RuntimeException(message.format(args)) {
		//
	}

	class OutOfManaException(message: String, args: Any*) extends RuntimeException(message.format(args)) {
		//
	}

	object Exception {

		def stackOverflow(allowedMaxSize: Int, requiredSize: Int): StackTooLargeException = {
			new StackTooLargeException("AllowedMax=%,d; Required=%,d;", allowedMaxSize, requiredSize)
		}

		def tooSmallStack(expectedSize: Int, actualSize: Int): StackTooSmallException = {
			new StackTooSmallException("Expected stack size=%,d; Actual stack Size=%,d;", expectedSize, actualSize)
		}

		def badJumpDestination(pc: Int): BadJumpDestinationException = {
			new BadJumpDestinationException("Bad jump destination: %s.".format(pc))
		}

		def notEnoughSpendingMana(cause: String, manaValue: Long, program: Program): OutOfManaException = {
			new OutOfManaException("Not enough mana for '%s' cause spending: invokeMana[%d], mana[%d], usedMana[%d];", cause, program.invoke.getMana.longValue, manaValue, program.result.manaUsed)
		}

	}

}

trait ProgramOutListener {
	def output(s: String)
}