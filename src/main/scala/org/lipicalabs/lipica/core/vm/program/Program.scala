package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.kernel.{EmptyAddress, Address, Payment, TransactionLike}
import org.lipicalabs.lipica.core.crypto.digest.{Digest256, DigestUtils}
import org.lipicalabs.lipica.core.utils.{BigIntBytes, UtilConsts, ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.PrecompiledContracts.PrecompiledContract
import org.lipicalabs.lipica.core.vm.trace.{ProgramTrace, ProgramTraceListener}
import org.lipicalabs.lipica.core.vm.{ManaCost, VM, DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.context.{ProgramContextFactory, ProgramContextFactoryImpl, ProgramContext}
import org.lipicalabs.lipica.core.vm.program.listener.ProgramListenerAware
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Program(private val ops: ImmutableBytes, private val context: ProgramContext, private val transaction: TransactionLike) {

	def this(_ops: ImmutableBytes, _context: ProgramContext) = this(_ops, _context, null)

	import Program._

	/** 各種リスナ。 */
	private var listener: ProgramOutListener = null
	private val traceListener = new ProgramTraceListener
	//private val programListener = CompositeProgramListener(Some(this.traceListener))

	/** プログラムカウンタ。 */
	private var pc = 0

	private val memory = setupProgramListener(new Memory)
	val stack = setupProgramListener(new Stack)
	val storage = setupProgramListener(Storage(context))

	val result = new ProgramResult
	val trace = new ProgramTrace(context)

	private val programContextFactory: ProgramContextFactory = new ProgramContextFactoryImpl

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

	def getCallDepth: Int = this.context.getCallDepth

	private def addInternalTx(nonce: BigIntBytes, manaLimit: DataWord, senderAddress: Address, receiveAddress: Address, value: BigInt, data: ImmutableBytes, note: String): InternalTransaction = {
		if (this.transaction ne null) {
			val senderNonce =
				if (nonce.isEmpty) {
					BigIntBytes(this.storage.getNonce(senderAddress))
				} else {
					nonce
				}
			this.result.addInternalTransaction(this.transaction.hash, getCallDepth, senderNonce, getManaPrice, manaLimit, senderAddress, receiveAddress, BigIntBytes(value), data, note)
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
		this.result.hReturn = v
	}

	/** メモリ操作 */
	def getMemSize: Int = this.memory.size

	def memorySave(addr: Int, value: ImmutableBytes, len: Int, limited: Boolean): Unit = {
		this.memory.write(addr, value, len, limited)
	}
	def memorySave(addr: DataWord, value: ImmutableBytes, limited: Boolean): Unit = {
		this.memory.write(addr.intValueSafe, value, value.length, limited)
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
		this.memory.readWord(addr.intValueSafe)
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
	def storageLoad(key: DataWord): Option[DataWord] = {
		this.storage.getStorageValue(getOwnerAddress.last20Bytes, key)
	}

	def suicide(obtainerAddress: DataWord): Unit = {
		val owner = getOwnerAddress.last20Bytes
		val obtainer = obtainerAddress.last20Bytes
		val balance = this.storage.getBalance(owner).getOrElse(UtilConsts.Zero)
		if (logger.isInfoEnabled) {
			logger.info("Transfer to [%s] heritage: [%s]".format(obtainer.toHexString, balance))
		}

		addInternalTx(BigIntBytes.empty, DataWord.Zero, owner, obtainer, balance, ImmutableBytes.empty, "suicide")
		Payment.transfer(this.storage, owner, obtainer, balance, Payment.Bequest)
		result.addDeletedAccount(getOwnerAddress)
	}

	def createContract(value: DataWord, memStart: DataWord, memSize: DataWord): Unit = {
		if (getCallDepth == MaxDepth) {
			stackPushZero()
			return
		}

		val senderAddress = this.getOwnerAddress.last20Bytes
		val endowment = value.value
		if (this.storage.getBalance(senderAddress).getOrElse(UtilConsts.Zero) < endowment) {
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
		val nonce = BigIntBytes(this.storage.getNonce(senderAddress))
		val newAddress = DigestUtils.computeNewAddress(getOwnerAddress.last20Bytes, nonce)

		//nonceを更新する。
		this.storage.increaseNonce(senderAddress)

		val track = this.storage.startTracking
		//既存だったら、残高を受け継ぐ。
		if (track.existsAccount(newAddress)) {
			val oldBalance = track.getBalance(newAddress).getOrElse(UtilConsts.Zero)
			track.createAccount(newAddress)
			track.addBalance(newAddress, oldBalance)
		} else {
			track.createAccount(newAddress)
		}
		//移動を実行する。
		val newBalance = Payment.transfer(track, senderAddress, newAddress, endowment, Payment.ContractCreationTx)
		//実行する。
		val internalTx = addInternalTx(nonce, getBlockManaLimit, senderAddress, EmptyAddress, endowment, programCode, "create")
		val programContext: ProgramContext = this.programContextFactory.createProgramContext(this, DataWord(newAddress.bytes), DataWord.Zero, manaLimit, newBalance, ImmutableBytes.empty, track, this.context.blockStore)

		val programResult =
			if (programCode.nonEmpty) {
				val vm = new VM
				val program = new Program(programCode, programContext, internalTx)
				vm.play(program)
				val localResult = program.result
				this.result.addInternalTransactions(result.internalTransactions)

				if (localResult.exception ne null) {
					//エラーが発生した。
					if (logger.isDebugEnabled) {
						logger.debug("<Program> Contract run halted by Exception: contract: [%s], exception: [%s]".format(newAddress.toHexString, localResult.exception))
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
		val contractCode = programResult.hReturn
		val storageCost = contractCode.length * ManaCost.CreateData
		val afterSpend = programContext.getMana.longValue - storageCost - programResult.manaUsed
		if (afterSpend < 0L) {
			//残金不足。
			track.saveCode(newAddress, ImmutableBytes.empty)
		} else {
			//料金を消費し、コントラクトのコードを保存する。
			programResult.spendMana(storageCost)
			track.saveCode(newAddress, contractCode)
		}

		track.commit()
		this.result.addDeletedAccounts(programResult.deletedAccounts)
		this.result.addLogs(programResult.logsAsSeq)

		//生成されたアドレスを、スタックにプッシュする。
		stackPush(DataWord(newAddress.bytes))

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
	def invokeContractCode(message: MessageCall): Unit = {
		if (getCallDepth == MaxDepth) {
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
			if (message.isStateless) {
				//呼び出し元の文脈を維持。
				senderAddress
			} else {
				//呼び出されるコードのアドレス。
				codeAddress
			}
		if (logger.isInfoEnabled) {
			logger.info("Call for existing contract: Stateless=%s, Address=[%s], OutDataOffset=[%s], OutDataSize=[%s]".format(message.isStateless, contextAddress.toHexString, message.outDataOffset, message.outDataSize))
		}
		val track = this.storage.startTracking
		//手数料。
		val endowment = message.endowment.value
		val senderBalance = track.getBalance(senderAddress).getOrElse(UtilConsts.Zero)
		if (senderBalance < endowment) {
			//手数料を払えない。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Refund mana from message call.")
			return
		}
		//コードを取得する。
		val programCode = this.storage.getCode(codeAddress).getOrElse(ImmutableBytes.empty)
		val contextBalance = Payment.transfer(track, senderAddress, contextAddress, endowment, Payment.ContractInvocationTx)

		//内部トランザクションを生成する。
		val internalTx = addInternalTx(BigIntBytes.empty, getBlockManaLimit, senderAddress, contextAddress, endowment, programCode, "call")

		val programResultOption =
			if (programCode.nonEmpty) {
				val programContext: ProgramContext = this.programContextFactory.createProgramContext(this, DataWord(contextAddress.bytes), message.endowment, message.mana, contextBalance, data, track, this.context.blockStore)

				val vm = new VM
				val program = new Program(programCode, programContext, internalTx)
				vm.play(program)
				val localResult = program.result

				this.trace.mergeToThis(program.trace)
				this.result.mergeToThis(localResult, mergeLogs = true)//TODO 仕様不明。

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
			val hReturn = programResultOption.get.hReturn
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
			this.refundMana(message.mana.longValue, "remaining mana from the internal call")
		}
	}

	/**
	 * 実装済みのコントラクトを実行します。
	 */
	def invokePrecompiledContractCode(message: MessageCall, contract: PrecompiledContract): Unit = {
		if (getCallDepth == MaxDepth) {
			//スタックの深さが限界。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Call deep limit reached.")
			return
		}

		val track = this.storage.startTracking
		val senderAddress = this.getOwnerAddress.last20Bytes
		val codeAddress = message.codeAddress.last20Bytes
		val contextAddress =
			if (message.isStateless) {
				senderAddress
			} else {
				codeAddress
			}

		//手数料。
		val endowment = message.endowment.value
		val senderBalance = track.getBalance(senderAddress).getOrElse(UtilConsts.Zero)
		if (senderBalance < endowment) {
			//手数料を払えない。
			stackPushZero()
			this.refundMana(message.mana.longValue, "Refund mana from message call.")
			return
		}
		//手数料を取る。
		Payment.transfer(track, senderAddress, contextAddress, endowment, Payment.ContractInvocationTx)

		//データに応じたコストを計算する。
		val data = this.memoryChunk(message.inDataOffset.intValue, message.inDataSize.intValue)
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
		if (manaLogger.isInfoEnabled) {
			manaLogger.info("[%s] Spent for cause: [%s], mana: [%,d]".format(context.hashCode, cause, manaValue))
		}
		if ((getMana.longValue - manaValue) < 0) {
			throw Exception.notEnoughSpendingMana(cause, manaValue, this)
		}
		this.result.spendMana(manaValue)
	}
	def spendAllMana(): Unit = {
		spendMana(getMana.longValue, "Spending all remaining")
	}
	def refundMana(manaValue: Long, cause: String): Unit = {
		if (manaLogger.isInfoEnabled) {
			manaLogger.info("[%s] Refund for cause: [%s], mana: [%,d]".format(context.hashCode, cause, manaValue))
		}
		this.result.refundMana(manaValue)
	}
	def futureRefundMana(manaValue: Long): Unit = {
		if (logger.isInfoEnabled) {
			logger.info("Future refund added: %,d".format(manaValue))
		}
		this.result.addFutureRefund(manaValue)
	}
	def resetFutureRefund(): Unit = this.result.resetFutureRefund()

	def getCode: ImmutableBytes = this.ops

	/**
	 * あるアドレスに結び付けられたコードをロードして返します。
	 */
	def getCodeAt(address: DataWord): Option[ImmutableBytes] = {
		this.context.getRepository.getCode(address.last20Bytes)
	}

	def getOwnerAddress: DataWord = this.context.getOwnerAddress

	def getBlockHash(index: Int): DataWord = {
		if ((index < this.getBlockNumber.longValue) && (256.max(this.getBlockNumber.intValue) - 256 <= index)) {
			//最近256ブロック内である。
			this.context.blockStore.getBlockHashByNumber(index, Digest256(getParentHash.data)).map(found => DataWord(found.bytes)).getOrElse(DataWord.Zero)
		} else {
			//古すぎるか未知。
			DataWord.Zero
		}
	}

	def getBalance(address: DataWord): DataWord = {
		val balance = this.storage.getBalance(address.last20Bytes).getOrElse(UtilConsts.Zero)
		DataWord(ImmutableBytes.asSignedByteArray(balance))
	}

	def getOriginAddress = this.context.getOriginAddress
	def getCallerAddress = this.context.getCallerAddress
	def getManaPrice = this.context.getMinManaPrice
	def getMana = {
		val remaining = context.getMana.longValue - this.result.manaUsed
		DataWord(remaining)
	}
	def getCallValue = this.context.getCallValue
	def getDataSize = this.context.getDataSize
	def getDataValue(index: DataWord) = this.context.getDataValue(index)
	def getDataCopy(offset: DataWord, length: DataWord): ImmutableBytes = this.context.getDataCopy(offset, length)
	def getParentHash = this.context.getParentHash
	def getCoinbase = this.context.getCoinbase
	def getTimestamp = this.context.getTimestamp
	def getBlockNumber = this.context.getBlockNumber
	def getDifficulty = this.context.getDifficulty
	def getBlockManaLimit = this.context.getBlockManaLimit

	def setRuntimeFailure(e: RuntimeException): Unit = {
		this.result.exception = e
	}

	def memoryToString: String = this.memory.toString

	def fullTrace(): Unit = {
		//TODO 未実装：fullTrace
	}

	//TODO stringify multiline
	//TODO stringify(not used)

	/**
	 * 現在のプログラムカウンタにおける文脈情報を、
	 * トレース領域に保存します。
	 */
	def saveOpTrace(): Unit = {
		if (this.pc < this.ops.length) {
			this.trace.addOp(this.ops(this.pc), this.pc, getCallDepth, getMana, this.traceListener.resetActions())
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
			}}
			i += 1
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

}

object Program {
	private val logger = LoggerFactory.getLogger("vm")
	private val manaLogger = LoggerFactory.getLogger("mana")

	/**
	 * LVMの関数呼び出しの限界となる深さ。
	 * JVMには -Xss10M 程度が必要になる。
	 */
	private val MaxDepth = 1024

	private val MaxStackSize = 1024

	class ProgramException(message: String) extends RuntimeException(message)

	/**
	 * スタックが予想されたほど深くない場合に発生する例外。
	 */
	class StackTooSmallException(message: String, args: Any*) extends ProgramException(message.format(args)) {
		//
	}

	/**
	 * スタックオーバーフロー時に発生する例外。
	 */
	class StackTooLargeException(message: String, args: Any*) extends ProgramException(message.format(args)) {
		//
	}

	/**
	 * JUMPもしくはJUMPI命令のジャンプ先が正当でない場合に発生する例外。
	 */
	class BadJumpDestinationException(message: String, args: Any*) extends ProgramException(message.format(args)) {
		//
	}

	/**
	 * 実行に必要なマナが枯渇した際に発生する例外。
	 */
	class OutOfManaException(message: String, args: Any*) extends ProgramException(message.format(args)) {
		//
	}

	class IllegalOperationException(message: String, args: Any*) extends ProgramException(message.format(args)) {
		//
	}

	object Exception {

		def stackOverflow(allowedMaxSize: Int, requiredSize: Int): StackTooLargeException = {
			new StackTooLargeException("AllowedMax=%,d; Required=%,d;".format(allowedMaxSize, requiredSize))
		}

		def tooSmallStack(expectedSize: Int, actualSize: Int): StackTooSmallException = {
			new StackTooSmallException("Expected stack size=%,d; Actual stack Size=%,d;".format(expectedSize, actualSize))
		}

		def badJumpDestination(pc: Int): BadJumpDestinationException = {
			new BadJumpDestinationException("Bad jump destination: %s.".format(pc))
		}

		def notEnoughSpendingMana(cause: String, manaValue: Long, program: Program): OutOfManaException = {
			new OutOfManaException("Not enough mana for '%s' cause spending: invokeMana[%d], mana[%d], usedMana[%d];".format(cause, program.context.getMana.longValue, manaValue, program.result.manaUsed))
		}

		def notEnoughOpMana(op: OpCode, opMana: BigInt, programMana: BigInt): OutOfManaException = {
			new OutOfManaException("Not enough mana for '%s' operation executing: opMana[%,d], programMana[%,d];".format(op, opMana, programMana))
		}

		def notEnoughOpMana(op: OpCode, opMana: DataWord, programMana: DataWord): OutOfManaException = {
			new OutOfManaException("Not enough mana for '%s' operation executing: opMana[%,d], programMana[%,d];".format(op, opMana.value, programMana.value))
		}

		def manaOverflow(actualMana: BigInt, manaLimit: BigInt): OutOfManaException = {
			new OutOfManaException("Mana value overflow: actualMana[%d], manaLimit[%d];".format(actualMana.longValue(), manaLimit.longValue()))
		}

		def invalidOpCode(opCode: Byte): IllegalOperationException = {
			new IllegalOperationException("Invalid operation code: opCode[%s];".format(opCode))
		}

	}

}

trait ProgramOutListener {
	def output(s: String)
}

