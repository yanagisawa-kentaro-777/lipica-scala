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
			//次に実行すべき１バイトに対応する。オペコードオブジェクトを取得する。
			val op = OpCode.code(program.getCurrentOp).orNull
			if (op eq null) {
				throw Program.Exception.invalidOpCode(program.getCurrentOp)
			}

			//文脈情報を保持し、スタックの深さを確認する。
			program.setLastOp(op.opcode)
			program.verifyStackSize(op.require)
			program.verifyStackOverflow(op.require, op.ret)

			val oldMemSize: Long = program.getMemSize
			val stack = program.stack

			var hint = ""
			val callMana: Long = 0
			var memWords: Long = 0
			val manaBefore: Long = program.getMana.longValue
			val stepBefore: Int = program.getPC

			//必要なマナ容量および新たに必要となるメモリサイズを計算する。
			val (manaCost, newMemSize, copySize) = computeNecessaryResources(op, program)
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

	private def computeNecessaryResources(op: OpCode, program: Program): (Long, BigInt, Long) = {
		val stack = program.stack
		op match {
			case Stop | Suicide =>
				(ManaCost.Stop, Zero, 0L)
			case SStore =>
				val newValue = stack.get(stack.size - 2)
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
				(ManaCost.Return, memNeeded(stack.peek, stack.get(stack.size - 2)), 0L)
			case SHA3 =>
				val size = stack.get(stack.size - 2)
				val chunkUsed = (size.longValue + DataWord.NUM_BYTES - 1) / DataWord.NUM_BYTES
				val manaCost = ManaCost.SHA3 + (chunkUsed * ManaCost.SHA3Word)
				(manaCost, memNeeded(stack.peek, size), 0L)
			case CallDataCopy =>
				val copySize = stack.get(stack.size - 3).longValue
				(op.tier.asInt, memNeeded(stack.peek, stack.get(stack.size - 3)), copySize)
			case CodeCopy =>
				val copySize = stack.get(stack.size - 3).longValue
				(op.tier.asInt, memNeeded(stack.peek, stack.get(stack.size - 3)), copySize)
			case ExtCodeCopy =>
				val copySize = stack.get(stack.size - 4).longValue
				(op.tier.asInt, memNeeded(stack.get(stack.size - 2), stack.get(stack.size - 4)), copySize)
			case Call | CallCode =>
				val callManaWord = stack.get(stack.size - 1)
				if (program.getMana.value < callManaWord.value) {
					throw Program.Exception.notEnoughOpMana(op, callManaWord, program.getMana)
				}
				var manaCost = ManaCost.Call + callManaWord.longValue
				val callAddressWord = stack.get(stack.size - 2)

				if ((op != CallCode) && !program.storage.existsAccount(callAddressWord.last20Bytes)) {
					manaCost += ManaCost.NewAccountCall
				}
				if (!stack.get(stack.size - 3).isZero) {
					manaCost += ManaCost.ValueTransferCall
				}
				val in = memNeeded(stack.get(stack.size - 4), stack.get(stack.size - 5))
				val out = memNeeded(stack.get(stack.size - 6), stack.get(stack.size - 7))
				(manaCost, in.max(out), 0L)
			case Create =>
				(ManaCost.Create, memNeeded(stack.get(stack.size - 2), stack.get(stack.size - 3)), 0L)
			case Log0 | Log1 | Log2 | Log3 | Log4 =>
				val numTopics = op.opcode - OpCode.Log0.opcode
				val dataSize = stack.get(stack.size - 2).value
				val dataCost = dataSize * BigInt(ManaCost.LogDataMana)
				if (program.getMana.value < dataCost) {
					throw Program.Exception.notEnoughOpMana(op, dataCost, program.getMana.value)
				}
				val manaCost = ManaCost.LogMana + ManaCost.LogTopicMana * numTopics + ManaCost.LogDataMana * stack.get(stack.size - 2).longValue
				(manaCost, memNeeded(stack.peek, stack.get(stack.size - 2)), 0L)
			case Exp =>
				val exp = stack.get(stack.size - 2)
				val bytesOccupied = exp.occupiedBytes
				(ManaCost.ExpMana + ManaCost.ExpByteMana * bytesOccupied, Zero, 0L)
			case _ => (op.tier.asInt, Zero, 0L)
		}
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

}

object VM {
	private val logger = LoggerFactory.getLogger("VM")
	private val dumpLogger = LoggerFactory.getLogger("dump")

	private val Zero = BigInt(0)
	private val _32_ = BigInt(32L)
	private val logString = "[%s]    Op: [%s]  Mana: [%s] Deep: [%,d]  Hint: [%s]"

	private val MaxMana = BigInt(Long.MaxValue)

}