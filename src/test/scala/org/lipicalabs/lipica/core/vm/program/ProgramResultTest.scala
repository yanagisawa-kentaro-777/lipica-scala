package org.lipicalabs.lipica.core.vm.program

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.kernel.EmptyAddress
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{LogInfo, VMWord}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/08
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class ProgramResultTest extends Specification {
	sequential


	"instance creation and attributes" should {
		"be right" in {
			val result = ProgramResult.createEmpty
			result.manaUsed mustEqual 0
			result.futureRefund mustEqual 0
			result.hReturn.isEmpty mustEqual true
			(result.exception eq null) mustEqual true
			result.internalTransactions.isEmpty mustEqual true
			result.deletedAccounts.isEmpty mustEqual true
			result.logsAsSeq.isEmpty mustEqual true

			result.spendMana(5)
			result.manaUsed mustEqual 5
			result.refundMana(3)
			result.manaUsed mustEqual 2

			result.addFutureRefund(7)
			result.futureRefund mustEqual 7
			result.resetFutureRefund()
			result.futureRefund mustEqual 0
			result.addFutureRefund(6)
			result.futureRefund mustEqual 6

			result.addDeletedAccount(VMWord.Zero)
			result.deletedAccounts.size mustEqual 1
			result.addLog(new LogInfo(EmptyAddress, Seq.empty, ImmutableBytes.empty))
			result.addLog(new LogInfo(EmptyAddress, Seq.empty, ImmutableBytes.empty))
			result.logsAsSeq.size mustEqual 2

			val result2 = ProgramResult.createEmpty
			result2.mergeToThis(result, mergeLogs = true)
			result.deletedAccounts.size mustEqual 1
			result.logsAsSeq.size mustEqual 2
			result2.futureRefund mustEqual 6
		}
	}

}
