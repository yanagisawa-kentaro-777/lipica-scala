package org.lipicalabs.lipica.core.net.transport

import java.net.InetSocketAddress

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 18:21
 * YANAGISAWA, Kentaro
 */
class PongMessage extends TransportMessage {

	private var _token: ImmutableBytes = null
	def token: ImmutableBytes = this._token

	private var _timestamp: Long = System.currentTimeMillis / 1000L
	def timestamp: Long = this._timestamp

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items

		val offset = if (items.size == 2) 0 else 1

		this._token = items(0 + offset).bytes
		this._timestamp = items(1 + offset).asPositiveLong
	}

	override def toString: String = {
		"[PongMessage] token=%s; timestamp=%,d. %s".format(
			this.token, this.timestamp, super.toString
		)
	}

}

object PongMessage {

	def create(token: ImmutableBytes, address: InetSocketAddress, privateKey: ECKey): PongMessage = {
		val timestamp = System.currentTimeMillis / 1000L

		val encodedAddress = RBACCodec.Encoder.encode(address.getAddress.getAddress)
		val encodedPort = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(address.getPort))
		val encodedDest = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedPort))

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedTimestamp = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(timestamp))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedDest, encodedToken, encodedTimestamp))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._timestamp = timestamp
		pong
	}

	def create(token: ImmutableBytes, privateKey: ECKey): PongMessage = {
		val timestamp = System.currentTimeMillis / 1000L

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedTimestamp = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(timestamp))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedToken, encodedTimestamp))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._timestamp = timestamp
		pong
	}

}