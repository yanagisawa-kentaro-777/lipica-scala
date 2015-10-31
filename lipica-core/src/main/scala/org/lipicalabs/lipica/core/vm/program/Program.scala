package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.utils.ByteUtils
import org.lipicalabs.lipica.core.vm.{DataWord, OpCode}
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvoke
import org.lipicalabs.lipica.core.vm.program.listener.ProgramListenerAware
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Program(private val ops: Array[Byte], private val invoke: ProgramInvoke) {

	import Program._

	/** プログラムカウンタ。 */
	private var pc = 0

	private val memory = setupProgramListener(new Memory)
	val stack = setupProgramListener(new Stack)
	private val storage = setupProgramListener(Storage(invoke))

	/**
	 * JUMPDEST命令がある箇所の索引。
	 */
	private var jumpDests: Set[Int] = Set.empty

	private var stopped = false

	/** 最終オペコード。 */
	private var lastOp = 0
	/** 前回実行されたオペコード。 */
	private var previouslyExecutedOp = 0

	//TODO traceの実装。


	precompile()

	private def setupProgramListener[T <: ProgramListenerAware](traceListenerAware: T): T = {
		//TODO programListener および trace listener について未実装。
		traceListenerAware
	}

	private def getCallDeep: Int = this.invoke.getCallDeep

	//TODO addInternalTx

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

	//TODO setHReturn

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

	private def precompile(): Unit = {
		var i = 0
		while (i < this.ops.length) {
			OpCode.code(this.ops(i)).foreach {eachOp => {
				if (eachOp == OpCode.JumpDest) {
					//記憶しておく。
					this.jumpDests = this.jumpDests + i
				}
				val code = eachOp.opcode.toInt
				if ((OpCode.Push1.opcode.toInt <= code) && (code <= OpCode.Push32.opcode.toInt)) {
					//Push対象は読み飛ばす。
					i += ((code - OpCode.Push1.opcode.toInt) + 1)
				}
				i += 1
			}}
		}
	}

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

	object Exception {

		def stackOverflow(allowedMaxSize: Int, requiredSize: Int): StackTooLargeException = {
			new StackTooLargeException("AllowedMax=%,d; Required=%,d;", allowedMaxSize, requiredSize)
		}

		def tooSmallStack(expectedSize: Int, actualSize: Int): StackTooSmallException = {
			new StackTooSmallException("Expected stack size=%,d; Actual stack Size=%,d;", expectedSize, actualSize)
		}

	}

}
