package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Hello
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 21:13
 * YANAGISAWA, Kentaro
 */
case class HelloMessage(p2pVersion: Byte, clientId: String, capabilities: Seq[Capability], listenPort: Int, nodeId: NodeId) extends P2PMessage {

	override def toEncodedBytes = {
		val encodedP2PVersion = RBACCodec.Encoder.encode(this.p2pVersion)
		val encodedClientId = RBACCodec.Encoder.encode(this.clientId)
		val encodedCapabilitiesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.capabilities.toSeq.map(_.toEncodedBytes))
		val encodedPort = RBACCodec.Encoder.encode(this.listenPort)
		val encodedNodeId = RBACCodec.Encoder.encode(this.nodeId)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedP2PVersion, encodedClientId, encodedCapabilitiesSeq, encodedPort, encodedNodeId))
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
		val nodeId = NodeId(items(4).bytes)
		HelloMessage(p2pVersion, clientId, capabilities, port, nodeId)
	}

}