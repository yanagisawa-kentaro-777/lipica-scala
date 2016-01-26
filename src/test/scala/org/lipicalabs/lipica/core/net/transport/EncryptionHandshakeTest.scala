package org.lipicalabs.lipica.core.net.transport

import java.math.BigInteger

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class EncryptionHandshakeTest extends Specification {
	sequential

	"AuthInitiateMessage" should {
		"be right" in {
			val initiator = EncryptionHandshake.createInitiator(new ECKey().decompress().getPubKeyPoint)
			val myKey = new ECKey().decompress()
			val message = initiator.createAuthInitiate(new Array[Byte](32), myKey)
			val encodedBytes = message.encode
			encodedBytes.length mustEqual AuthInitiateMessage.length
			val rebuilt = AuthInitiateMessage.decode(encodedBytes)
			rebuilt.ephemeralPublicHash mustEqual message.ephemeralPublicHash
			rebuilt.encode mustEqual encodedBytes
		}
	}

	"agreement (1)" should {
		"be right" in {
			val responder = EncryptionHandshake.createResponder
			val remoteKey = new ECKey().decompress()
			val initiator = EncryptionHandshake.createInitiator(remoteKey.getPubKeyPoint)
			val myKey = new ECKey().decompress()
			val initiateMessage = initiator.createAuthInitiate(myKey)
			val initiatePacket = initiator.encryptAuthInitiate(initiateMessage)
			val responsePacket = responder.handleAuthInitiate(initiatePacket, remoteKey)
			initiator.handleAuthResponse(myKey, initiatePacket, responsePacket)

			initiator.secrets.aes mustEqual responder.secrets.aes
			initiator.secrets.mac mustEqual responder.secrets.mac
			initiator.secrets.token mustEqual responder.secrets.token
		}
	}


}
