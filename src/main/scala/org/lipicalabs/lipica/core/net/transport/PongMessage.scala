package org.lipicalabs.lipica.core.net.transport

import java.net.InetAddress

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

	private var _expires: Long = 0L
	def expires: Long = this._expires

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items

		val offset = if (items.size == 2) 0 else 1

		this._token = items(0 + offset).bytes
		this._expires = items(1 + offset).asPositiveLong
	}

	override def toString: String = {
		"[PongMessage] token=%s, expires in %,d seconds. %s".format(
			this.token, this.expires - (System.currentTimeMillis / 1000L), super.toString
		)
	}

}

object PongMessage {

	def create(token: ImmutableBytes, address: InetAddress, port: Int, privateKey: ECKey): PongMessage = {
		val expiration = 60 + System.currentTimeMillis / 1000L

		val encodedAddress = RBACCodec.Encoder.encode(address.getAddress)
		val encodedPort = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(port))
		val encodedTo = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedPort))

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedTo, encodedToken, encodedExpiration))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._expires = expiration
		pong
	}

	def create(token: ImmutableBytes, privateKey: ECKey): PongMessage = {
		val expiration = 3 + System.currentTimeMillis / 1000L

		val encodedToken = RBACCodec.Encoder.encode(token)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](2)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedToken, encodedExpiration))

		val pong: PongMessage = TransportMessage.encode(messageType, data, privateKey)
		pong._token = token
		pong._expires = expiration
		pong
	}

}