package org.lipicalabs.lipica.core.net.peer_discovery

import java.math.BigInteger
import java.net.{InetAddress, InetSocketAddress}

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.lipicalabs.lipica.core.net.peer_discovery.message._
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class PeerDiscoveryMessagesTest extends Specification {
	sequential

	"test PingMessage" should {
		"be right" in {
			val message1 = new PingMessage
			message1.address = InetAddress.getByName("192.168.100.0")
			message1.port = 32000
			val key = ECKeyPair.fromPrivateKey(BigInteger.TEN)
			val address = new InetSocketAddress(message1.address, message1.port)
			val message2 = PingMessage.create(address, address, key)

			val message3: PingMessage = AbstractPeerDiscoveryMessage.decode(message2.packet).right.get

			message1.address mustEqual message2.address
			message2.address mustEqual message3.address
			message3.address mustEqual InetAddress.getByName("192.168.100.0")

			message1.port mustEqual message2.port
			message2.port mustEqual message3.port
			message3.port mustEqual 32000
		}
	}

	"test PongMessage" should {
		"be right" in {
			val wire = ImmutableBytes.parseHexString("84db9bf6a1f7a3444f4d4946155da16c63a51abdd6822ac683d8243f260b99b265601b769acebfe3c76ddeb6e83e924f2bac2beca0c802ff0745d349bd58bc6662d62d38c2a3bb3e167a333d7d099496ebd35e096c5c1ee1587e9bd11f20e3d80002e6a079d49bdba3a7acfc9a2881d768d1aa246c2486ab166f0305a863bd47c5d21e0e8454f8483c").toByteArray

			val message1: PongMessage = AbstractPeerDiscoveryMessage.decode(wire).right.get

			val key = ECKeyPair.fromPrivateKey(BigInteger.TEN)
			val message2 = PongMessage.create(message1.token, key)
			val message3: PongMessage = AbstractPeerDiscoveryMessage.decode(message2.packet).right.get

			message1.token mustEqual message2.token
			message2.token mustEqual message3.token
		}
	}

	"test FindNodeMessage" should {
		"be right" in {
			val wire = ImmutableBytes.parseHexString("3770d98825a42cb69edf70ffdf8d6d2b28a8c5499a7e3350e4a42c94652339cac3f8e9c3b5a181c8dd13e491ad9229f6a8bd018d786e1fb9e5264f43bbd6ce93af9bc85b468dee651bcd518561f83cb166da7aef7e506057dc2fbb2ea582bcc00003f847b84083fba54f6bb80ce31f6d5d1ec0a9a2e4685bc185115b01da6dcb70cd13116a6bd08b86ffe60b7d7ea56c6498848e3741113f8e70b9f0d12dbfe895680d03fd658454f6e772").toByteArray

			val message1: FindNodeMessage = AbstractPeerDiscoveryMessage.decode(wire).right.get

			val key = ECKeyPair.fromPrivateKey(BigInteger.TEN)
			val message2 = FindNodeMessage.create(message1.target, key)
			val message3: FindNodeMessage = AbstractPeerDiscoveryMessage.decode(message2.packet).right.get

			message1.target mustEqual message2.target
			message2.target mustEqual message3.target
		}
	}

	"test NeighborsMessage" should {
		"be right" in {
			//TODO この仕様は廃絶。
			val wire = ImmutableBytes.parseHexString("d5106e888eeca1e0b4a93bf17c325f912b43ca4176a000966619aa6a96ac9d5a60e66c73ed5629c13d4d0c806a3127379541e8d90d7fcb52c33c5e36557ad92dfed9619fcd3b92e42683aed89bd3c6eef6b59bd0237c36d83ebb0075a59903f50104f90200f901f8f8528c38352e36352e31392e32333182f310b840aeb2dd107edd996adf1bbf835fb3f9a11aabb7ed3dfef84c7a3c8767482bff522906a11e8cddee969153bf5944e64e37943db509bb4cc714c217f20483802ec0f8528c38352e36352e31392e32333182e5b4b840b70cdf8f23024a65afbf12110ca06fa5c37bd9fe4f6234a0120cdaaf16e8bb96d090d0164c316aaa18158d346e9b0a29ad9bfa0404ab4ee9906adfbacb01c21bf8528c38352e36352e31392e32333182df38b840ed8e01b5f5468f32de23a7524af1b35605ffd7cdb79af4eacd522c94f8ed849bb81dfed4992c179caeef0952ecad2d868503164a434c300356b369a33c159289f8528c38352e36352e31392e32333182df38b840136996f11c2c80f231987fc4f0cbd061cb021c63afaf5dd879e7c851a57be8d023af14bc201be81588ecab7971693b3f689a4854df74ad2e2334e88ae76aa122f8528c38352e36352e31392e32333182f303b840742eac32e1e2343b89c03a20fc051854ea6a3ff28ca918d1994fe1e32d6d77ab63352131db3ed0e7d6cc057d859c114b102f49052daee3d1c5f5fdaab972e655f8528c38352e36352e31392e32333182f310b8407d9e1f9ceb66fc21787b830554d604f933be203be9366710fb33355975e874a72b87837cf28b1b9ae171826b64e3c5d178326cbf71f89b3dec614816a1a40ce38454f6b578").toByteArray

			val message1: NeighborsMessage = AbstractPeerDiscoveryMessage.decode(wire).right.get

			val key = ECKeyPair.fromPrivateKey(BigInteger.TEN)
			val message2 = NeighborsMessage.create(message1.nodes, key)
			val message3: NeighborsMessage = AbstractPeerDiscoveryMessage.decode(message2.packet).right.get

			message1.nodes.indices.foreach {
				idx => {
					val node1 = message1.nodes(idx)
					val node3 = message3.nodes(idx)

					node1 mustEqual node3
				}
			}
			message1.nodes.size mustEqual message3.nodes.size
		}
	}
}
