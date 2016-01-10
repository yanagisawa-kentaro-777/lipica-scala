package org.lipicalabs.lipica.core.kernel

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.UtilConsts
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class AccountStateTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val accountState = new AccountState(UtilConsts.Zero, BigInt(2).pow(200))
			val expected = "f85e809" + "a0100000000000000000000000000000000000000000000000000" + "a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421" + "a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"
			accountState.encode.toHexString mustEqual expected

			val balance = accountState.addToBalance(BigInt(1000))
			accountState.subtractFromBalance(BigInt(500)) mustEqual balance - BigInt(500)

			accountState.toString.nonEmpty mustEqual true
		}
	}

}
