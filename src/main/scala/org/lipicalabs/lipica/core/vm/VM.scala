package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.config.SystemProperties
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
			val op = OpCode.code(program.getCurrentOp).orNull
			if (op eq null) {
				throw Program.Exception.invalidOpCode(program.getCurrentOp)
			}

			program.setLastOp(op.opcode)
			program.verifyStackSize(op.require)
			program.verifyStackOverflow(op.require, op.ret)

			val oldMemSize: Long = program.getMemSize
			var copySize: Long = 0
			val stack = program.stack

			var hint = ""
			val callMana: Long = 0
			var memWords: Long = 0
			val manaBefore: Long = program.getMana.longValue
			val stepBefore: Int = program.getPC

			val (manaCost, newMemSize) = computeNecessaryResources(op, program)
			//マナを消費する。
			program.spendMana(manaCost, op.toString)
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

	private def computeNecessaryResources(op: OpCode, program: Program): (Int, BigInt) = {
		val stack = program.stack
		op match {
			case Stop |
			     Suicide => (ManaCost.Stop, Zero)
			case _ => (op.tier.asInt, Zero)
		}
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

}

object VM {
	private val logger = LoggerFactory.getLogger("VM")
	private val dumpLogger = LoggerFactory.getLogger("dump")

	private val Zero = BigInt(0)
	private val _32_ = BigInt(32L)
	private val logString = "[%s]    Op: [%s]  Mana: [%s] Deep: [%,d]  Hint: [%s]"

	private val MaxMana = BigInt(Long.MaxValue)

}