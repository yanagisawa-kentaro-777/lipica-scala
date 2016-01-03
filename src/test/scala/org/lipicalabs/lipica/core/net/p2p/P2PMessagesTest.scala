package org.lipicalabs.lipica.core.net.p2p

import java.net.InetAddress

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.message.ReasonCode
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class P2PMessagesTest extends Specification {
	sequential

	"test HelloMessage" should {
		"be right" in {
			val message = HelloMessage(1, "client", Seq(Capability("a", 2)), 1000, ImmutableBytes.create(64))
			val encoded = message.toEncodedBytes
			val decoded: HelloMessage = decodeMessage(P2PMessageCode.Hello.asByte, encoded)

			decoded.code mustEqual message.code
			decoded.p2pVersion mustEqual 1
			decoded.clientId mustEqual "client"
			decoded.capabilities.size mustEqual 1
			decoded.capabilities.head.name mustEqual "a"
			decoded.capabilities.head.version mustEqual 2
			decoded.listenPort mustEqual 1000
			decoded.peerId mustEqual ImmutableBytes.create(64)
		}
	}

	"test DisconnectMessage (0)" should {
		"be right" in {
			ReasonCode.all.values.foreach {
				originalReason => {
					val message = DisconnectMessage(originalReason)
					val encoded = message.toEncodedBytes
					val decoded: DisconnectMessage = decodeMessage(P2PMessageCode.Disconnect.asByte, encoded)

					decoded.code mustEqual message.code
					decoded.reason mustEqual originalReason
				}
			}
			ok
		}
	}

	"test DisconnectMessage (1)" should {
		"be right" in {
			val message = DisconnectMessage(ReasonCode.Requested)
			val encoded = message.toEncodedBytes
			val decoded: DisconnectMessage = decodeMessage(P2PMessageCode.Disconnect.asByte, encoded)

			decoded.reason mustEqual ReasonCode.Requested
		}
	}

	"test DisconnectMessage (1.5)" should {
		"be right" in {
			val message = DisconnectMessage.decode(ImmutableBytes.parseHexString("C100"))
			val encoded = message.toEncodedBytes
			val decoded: DisconnectMessage = decodeMessage(P2PMessageCode.Disconnect.asByte, encoded)

			decoded.reason mustEqual ReasonCode.Requested
		}
	}

	"test DisconnectMessage (2)" should {
		"be right" in {
			val message = DisconnectMessage(ReasonCode.LocalIdentity)
			val encoded = message.toEncodedBytes
			val decoded: DisconnectMessage = decodeMessage(P2PMessageCode.Disconnect.asByte, encoded)

			decoded.reason mustEqual ReasonCode.LocalIdentity
		}
	}

	"test GetPeersMessage" should {
		"be right" in {
			val message = GetPeersMessage()
			val encoded = message.toEncodedBytes
			val decoded: GetPeersMessage = decodeMessage(P2PMessageCode.GetPeers.asByte, encoded)

			decoded.code mustEqual message.code
		}
	}

	"test PeersMessage" should {
		"be right" in {
			val message = PeersMessage(Set(Peer(InetAddress.getByAddress(Array[Byte](192.toByte, 168.toByte, 100.toByte, 101.toByte)), 123, ImmutableBytes.parseHexString("0123456789"), Seq(Capability("a", 2)))))
			val encoded = message.toEncodedBytes
			val decoded: PeersMessage = decodeMessage(P2PMessageCode.Peers.asByte, encoded)

			decoded.code mustEqual message.code
			decoded.peers.size mustEqual message.peers.size
			(decoded.peers == message.peers) mustEqual true
		}
	}

	"test PingMessage" should {
		"be right" in {
			val message = PingMessage()
			val encoded = message.toEncodedBytes
			val decoded: PingMessage = decodeMessage(P2PMessageCode.Ping.asByte, encoded)

			decoded.code mustEqual message.code
		}
	}

	"test PongMessage" should {
		"be right" in {
			val message = PongMessage()
			val encoded = message.toEncodedBytes
			val decoded: PongMessage = decodeMessage(P2PMessageCode.Pong.asByte, encoded)

			decoded.code mustEqual message.code
		}
	}

	private def decodeMessage[T](byte: Byte, bytes: ImmutableBytes): T = {
		(new P2PMessageFactory).create(byte, bytes).get.asInstanceOf[T]
	}
}
