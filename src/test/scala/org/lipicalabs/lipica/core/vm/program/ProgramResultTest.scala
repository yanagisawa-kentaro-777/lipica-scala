package org.lipicalabs.lipica.core.vm.program

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{LogInfo, DataWord}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
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
			result.logInfoList.isEmpty mustEqual true

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

			result.addDeletedAccount(DataWord.Zero)
			result.deletedAccounts.size mustEqual 1
			result.addLogInfo(new LogInfo(ImmutableBytes.empty, Seq.empty, ImmutableBytes.empty))
			result.addLogInfo(new LogInfo(ImmutableBytes.empty, Seq.empty, ImmutableBytes.empty))
			result.logInfoList.size mustEqual 2

			val result2 = ProgramResult.createEmpty
			result2.mergeToThis(result)
			result.deletedAccounts.size mustEqual 1
			result.logInfoList.size mustEqual 2
			result2.futureRefund mustEqual 6
		}
	}

}
