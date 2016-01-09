package org.lipicalabs.lipica.core.net.transport

import java.nio.charset.StandardCharsets

import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}

/**
 *
 * @since 2015/12/18
 * @author YANAGISAWA, Kentaro
 */
class HandshakeMessage(val version: Long, val name: String, val capabilities: Seq[Capability], val listenPort: Long, val nodeId: ImmutableBytes) {

	def encode: Array[Byte] = {
		val encodedCapabilities = this.capabilities.map(_.toEncodedBytes)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(
			RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(this.version)),
			RBACCodec.Encoder.encode(this.name.getBytes(StandardCharsets.UTF_8)),
			RBACCodec.Encoder.encodeSeqOfByteArrays(encodedCapabilities),
			RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(this.listenPort)),
			RBACCodec.Encoder.encode(this.nodeId)
		)).toByteArray
	}
}

object HandshakeMessage {
	val HandshakeMessageType = 0x00

	def decode(wire: Array[Byte]): HandshakeMessage = {
		val items = RBACCodec.Decoder.decode(wire).right.get.items
		val version = items.head.asPositiveLong
		val name = items(1).asString
		val capabilities = items(2).items.map(each => Capability.decode(each.items))
		val listenPort = items(3).asPositiveLong
		val nodeId = items(4).bytes
		new HandshakeMessage(version, name, capabilities, listenPort, nodeId)
	}
}