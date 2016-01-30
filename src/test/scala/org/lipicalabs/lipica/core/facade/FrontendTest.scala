package org.lipicalabs.lipica.core.facade


import java.security.SecureRandom

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.lipicalabs.lipica.core.kernel.{Transaction, Address160}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class FrontendTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val keyPair = ECKeyPair(new SecureRandom)
			val privateKey = ImmutableBytes.asUnsignedByteArray(keyPair.privateKey)
			val senderAddress = keyPair.toAddress

			val receiveAddress = Address160(ImmutableBytes.createRandom(new java.util.Random(), 20))
			val tx = LipicaImpl.createTransaction(BigInt(0), BigInt(7000000), BigInt(100000000), receiveAddress, BigInt(300000000), ImmutableBytes.empty)
			tx.sign(privateKey)

			val encoded = tx.toEncodedBytes
			val decoded = Transaction.decode(encoded)

			decoded.senderAddress mustEqual senderAddress
		}
	}

}
