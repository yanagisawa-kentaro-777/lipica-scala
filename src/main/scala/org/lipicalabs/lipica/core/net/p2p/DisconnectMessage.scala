package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Disconnect
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * 接続切断を予告するメッセージです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/05 12:38
 * YANAGISAWA, Kentaro
 */
case class DisconnectMessage(reason: ReasonCode) extends P2PMessage {
	override def toEncodedBytes = {
		val encodedReason = RBACCodec.Encoder.encode(this.reason.asByte)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedReason))
	}
	override def code = Disconnect.asByte
}

object DisconnectMessage {
	def decode(encodedBytes: ImmutableBytes): DisconnectMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val reasonAsByte =
			if (items.head.bytes.isEmpty) {
				0.toByte
			} else {
				items.head.bytes.head
			}
		val reason = ReasonCode.fromInt(reasonAsByte & 0xFF)
		DisconnectMessage(reason)
	}
}