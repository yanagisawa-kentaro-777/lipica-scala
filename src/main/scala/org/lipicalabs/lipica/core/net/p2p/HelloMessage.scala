package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Hello
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 21:13
 * YANAGISAWA, Kentaro
 */
class HelloMessage(val p2pVersion: Byte, val clientId: String, val capabilities: Seq[Capability], val listenPort: Int, val peerId: String) extends P2PMessage {

	override def toEncodedBytes = {
		val encodedP2PVersion = RBACCodec.Encoder.encode(this.p2pVersion)
		val encodedClientId = RBACCodec.Encoder.encode(this.clientId)
		val encodedCapabilitiesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.capabilities.toSeq.map(_.toEncodedBytes))
		val encodedPort = RBACCodec.Encoder.encode(this.listenPort)
		val encodedPeerId = RBACCodec.Encoder.encode(this.peerId)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedP2PVersion, encodedClientId, encodedCapabilitiesSeq, encodedPort, encodedPeerId))
	}

	override val code = Hello.asByte
}

object HelloMessage {

	def decode(encodedBytes: ImmutableBytes): HelloMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val p2pVersion = items.head.asByte
		val clientId = items(1).asString
		val capabilities = items(2).items.map(each => Capability.decode(each.items))
		val port = items(3).asInt
		val peerId = items(4).asString
		new HelloMessage(p2pVersion, clientId, capabilities, port, peerId)
	}

}