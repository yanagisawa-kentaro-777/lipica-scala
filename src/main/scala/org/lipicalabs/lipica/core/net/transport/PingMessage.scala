package org.lipicalabs.lipica.core.net.transport

import java.nio.charset.StandardCharsets

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 15:14
 * YANAGISAWA, Kentaro
 */
class PingMessage extends TransportMessage {

	private var _host: String = null
	def host: String = this._host
	def host_=(v: String): Unit = this._host = v

	private var _port: Int = 0
	def port: Int = this._port
	def port_=(v: Int): Unit = this._port = v

	private var _expires: Long = 0L
	def expires: Long = this._expires

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		val fromSeq = items(1).items
		this._host = fromSeq.head.asString
		this._port = fromSeq(1).asInt
		this._expires = items(3).asPositiveLong
	}

	override def toString: String = {
		"[PingMessage] host=%s, port=%d, expires in %,d seconds. %s".format(
			this.host, this.port, this.expires - (System.currentTimeMillis / 1000L), super.toString
		)
	}
}

object PingMessage {

	def create(host: String, port: Int, privateKey: ECKey): PingMessage = {
		val expiration = 60 + System.currentTimeMillis / 1000L

		val hostBytes = host.getBytes(StandardCharsets.UTF_8)
		val encodedHost = RBACCodec.Encoder.encode(hostBytes)
		val portBytes = ByteUtils.toByteArrayWithNoLeadingZeros(port)
		val encodedPort = RBACCodec.Encoder.encode(portBytes)

		val encodedHostTo = RBACCodec.Encoder.encode(hostBytes)
		val encodedPortTo = RBACCodec.Encoder.encode(portBytes)

		val expirationBytes = ByteUtils.toByteArrayWithNoLeadingZeros(expiration)
		val encodedExpiration = RBACCodec.Encoder.encode(expirationBytes)

		val messageType = Array[Byte](1)
		val encodedVersion = RBACCodec.Encoder.encode(Array[Byte](4))
		val fromSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHost, encodedPort, encodedPort))
		val toSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHostTo, encodedPortTo, encodedPortTo))

		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedVersion, fromSeq, toSeq, encodedExpiration))

		val result: PingMessage = TransportMessage.encode(messageType, data, privateKey)
		result._expires = expiration
		result._host = host
		result._port = port
		result
	}

}
