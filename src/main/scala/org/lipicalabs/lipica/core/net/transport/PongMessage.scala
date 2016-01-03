package org.lipicalabs.lipica.core.net.transport

import java.net.InetSocketAddress

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 18:21
 * YANAGISAWA, Kentaro
 */
class PongMessage extends TransportMessage {

	private var _token: ImmutableBytes = null
	def token: ImmutableBytes = this._token

	private var _expiration: Long = (System.currentTimeMillis / 1000L) + 60L
	def expiration: Long = this._expiration

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items

		val offset = if (items.size == 2) 0 else 1

		this._token = items(0 + offset).bytes
		this._expiration = items(1 + offset).asPositiveLong
	}

	override def toString: String = {
		"[PongMessage] token=%s; expiration=%,d. %s".format(
			this.token, this.expiration, super.toString
		)
	}

}

object PongMessage {

	def create(token: ImmutableBytes, address: InetSocketAddress, privateKey: ECKey): PongMessage = {
		val expiration = (System.currentTimeMillis / 1000L) + 60L

		val encodedAddress = RBACCodec.Encoder.encode(address.getAddress.getAddress)
		val encodedPort = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(address.getPort))
		val encodedDest = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedPort))

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedDest, encodedToken, encodedExpiration))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._expiration = expiration
		pong
	}

	def create(token: ImmutableBytes, privateKey: ECKey): PongMessage = {
		val expiration = (System.currentTimeMillis / 1000L) + 60L

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedToken, encodedExpiration))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._expiration = expiration
		pong
	}

}