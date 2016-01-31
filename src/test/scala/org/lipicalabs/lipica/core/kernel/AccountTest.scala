package org.lipicalabs.lipica.core.kernel


import java.security.SecureRandom

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class AccountTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val account = new Account(ECKeyPair(new SecureRandom))
			account.address.length mustEqual 20
		}

	}

}
