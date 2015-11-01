package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.base.{Repository, TransactionLike}
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.trace.{ProgramTrace, ProgramTraceListener}
import org.lipicalabs.lipica.core.vm.{DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvoke
import org.lipicalabs.lipica.core.vm.program.listener.{CompositeProgramListener, ProgramListenerAware}
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Program(private val ops: Array[Byte], private val invoke: ProgramInvoke, private val transaction: TransactionLike) {

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

	private def getCallDeep: Int = this.invoke.getCallDeep

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
	def sweep(n: Int): Array[Byte] = {
		if (this.ops.length <= this.pc + n) {
			stop()
		}
		val result = java.util.Arrays.copyOfRange(this.ops, this.pc, this.pc + n)
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
	/**
	 * テスト用に、メモリの中身をコピーして返します。
	 */
	private[vm] def getMemoryContent: ImmutableBytes = {
		this.memory.read(0, memory.size)
	}
	/**
	 * テスト用に、メモリの中身を渡された内容に書き換えます。
	 */
	private[vm] def initMemory(data: ImmutableBytes): Unit = {
		this.memory.write(0, data, data.length, limited = false)
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


	def getCodeAt(address: DataWord): Array[Byte] = {
		val code = this.invoke.getRepository.getCode(address.last20Bytes)
		ByteUtils.launderNullToEmpty(code)
	}
	def getOwnerAddress: DataWord = this.invoke.getOwnerAddress

	//TODO getBlockHash

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
	def storageLoad(key: DataWord): DataWord = {
		this.storage.getStorageValue(getOwnerAddress.last20Bytes, key)
	}
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

	}

}

trait ProgramOutListener {
	def output(s: String)
}