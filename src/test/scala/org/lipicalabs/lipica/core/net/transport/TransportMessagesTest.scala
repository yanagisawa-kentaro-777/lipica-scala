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
class TransportMessagesTest extends Specification {
	sequential

	"test PingMessage" should {
		"be right" in {
			val message1 = new PingMessage
			message1.host = "192.168.100.0"
			message1.port = 32000
			val key = ECKey.fromPrivate(BigInteger.TEN)
			val message2 = PingMessage.create(message1.host, message1.port, key)

			val message3: PingMessage = TransportMessage.decode(message2.packet)

			message1.host mustEqual message2.host
			message2.host mustEqual message3.host
			message3.host mustEqual "192.168.100.0"

			message1.port mustEqual message2.port
			message2.port mustEqual message3.port
			message3.port mustEqual 32000
		}
	}

	"test PongMessage" should {
		"be right" in {
			val wire = ImmutableBytes.parseHexString("84db9bf6a1f7a3444f4d4946155da16c63a51abdd6822ac683d8243f260b99b265601b769acebfe3c76ddeb6e83e924f2bac2beca0c802ff0745d349bd58bc6662d62d38c2a3bb3e167a333d7d099496ebd35e096c5c1ee1587e9bd11f20e3d80002e6a079d49bdba3a7acfc9a2881d768d1aa246c2486ab166f0305a863bd47c5d21e0e8454f8483c").toByteArray

			val message1: PongMessage = TransportMessage.decode(wire)

			val key = ECKey.fromPrivate(BigInteger.TEN)
			val message2 = PongMessage.create(message1.token, key)
			val message3: PongMessage = TransportMessage.decode(message2.packet)

			message1.token mustEqual message2.token
			message2.token mustEqual message3.token
		}
	}

	"test FindNodeMessage" should {
		"be right" in {
			val wire = ImmutableBytes.parseHexString("3770d98825a42cb69edf70ffdf8d6d2b28a8c5499a7e3350e4a42c94652339cac3f8e9c3b5a181c8dd13e491ad9229f6a8bd018d786e1fb9e5264f43bbd6ce93af9bc85b468dee651bcd518561f83cb166da7aef7e506057dc2fbb2ea582bcc00003f847b84083fba54f6bb80ce31f6d5d1ec0a9a2e4685bc185115b01da6dcb70cd13116a6bd08b86ffe60b7d7ea56c6498848e3741113f8e70b9f0d12dbfe895680d03fd658454f6e772").toByteArray

			val message1: FindNodeMessage = TransportMessage.decode(wire)

			val key = ECKey.fromPrivate(BigInteger.TEN)
			val message2 = FindNodeMessage.create(message1.target, key)
			val message3: FindNodeMessage = TransportMessage.decode(message2.packet)

			message1.target mustEqual message2.target
			message2.target mustEqual message3.target
		}
	}
}
