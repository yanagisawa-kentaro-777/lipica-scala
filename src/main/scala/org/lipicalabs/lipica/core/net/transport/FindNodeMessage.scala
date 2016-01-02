package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 19:19
 * YANAGISAWA, Kentaro
 */
class FindNodeMessage extends TransportMessage {

	private var _target: ImmutableBytes = null
	def target: ImmutableBytes = this._target

	private var _timestamp: Long = System.currentTimeMillis / 1000L
	def timestamp: Long = this._timestamp

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		this._target = items.head.bytes
		this._timestamp = items(1).asPositiveLong
	}

	override def toString: String = {
		"[FindNodeMessage] target=%s; timestamp=%,d. %s".format(
			this.target, this.timestamp, super.toString
		)
	}
}

object FindNodeMessage {

	def create(target: ImmutableBytes, privateKey: ECKey): FindNodeMessage = {
		val timestamp = System.currentTimeMillis / 1000L

		val encodedTarget = RBACCodec.Encoder.encode(target)
		val encodedTimestamp = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(timestamp))

		val messageType = Array[Byte](3)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedTarget, encodedTimestamp))

		val result: FindNodeMessage = TransportMessage.encode(messageType, data, privateKey)
		result._target = target
		result._timestamp = timestamp
		result
	}

}