package org.lipicalabs.lipica.core.net.peer_discovery.message

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.net.transport.TransportMessage
import org.lipicalabs.lipica.core.utils.ByteUtils

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 19:19
 * YANAGISAWA, Kentaro
 */
class FindNodeMessage extends TransportMessage {

	private var _target: NodeId = null
	def target: NodeId = this._target

	private var _expiration: Long = (System.currentTimeMillis / 1000L) + 60L
	def expiration: Long = this._expiration

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		this._target = NodeId(items.head.bytes)
		this._expiration = items(1).asPositiveLong
	}

	override def toString: String = {
		"[FindNodeMessage] target=%s; expiration=%,d. %s".format(
			this.target, this.expiration, super.toString
		)
	}
}

object FindNodeMessage {

	def create(target: NodeId, privateKey: ECKey): FindNodeMessage = {
		val expiration = (System.currentTimeMillis / 1000L) + 60L

		val encodedTarget = RBACCodec.Encoder.encode(target)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](3)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedTarget, encodedExpiration))

		val result: FindNodeMessage = TransportMessage.encode(messageType, data, privateKey)
		result._target = target
		result._expiration = expiration
		result
	}

}