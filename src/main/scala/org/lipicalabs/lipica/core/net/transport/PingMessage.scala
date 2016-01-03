package org.lipicalabs.lipica.core.net.transport

import java.net.{InetSocketAddress, InetAddress}

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ByteUtils

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 15:14
 * YANAGISAWA, Kentaro
 */
class PingMessage extends TransportMessage {

	private var _address: InetAddress = null
	def address: InetAddress = this._address
	def address_=(v: InetAddress): Unit = this._address = v

	private var _port: Int = 0
	def port: Int = this._port
	def port_=(v: Int): Unit = this._port = v

	private var _expiration: Long = (System.currentTimeMillis / 1000L) + 60L
	def expiration: Long = this._expiration

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		val fromSeq = items(1).items
		this._address = InetAddress.getByAddress(fromSeq.head.bytes.toByteArray)
		this._port = fromSeq(1).asInt
		this._expiration = items(3).asPositiveLong
	}

	override def toString: String = {
		"[PingMessage] Address=%s; Port=%d; Expiration=%,d. %s".format(
			this.address, this.port, this.expiration, super.toString
		)
	}
}

object PingMessage {

	def create(srcAddress: InetSocketAddress, destAddress: InetSocketAddress,  privateKey: ECKey): PingMessage = {
		val expiration = (System.currentTimeMillis / 1000L) + 60L

		val encodedSrcAddress = RBACCodec.Encoder.encode(srcAddress.getAddress.getAddress)
		val encodedSrcPort = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(srcAddress.getPort))

		val encodedDestAddress = RBACCodec.Encoder.encode(destAddress.getAddress.getAddress)
		val encodedDestPort = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(destAddress.getPort))

		val expirationBytes = ByteUtils.toByteArrayWithNoLeadingZeros(expiration)
		val encodedExpiration = RBACCodec.Encoder.encode(expirationBytes)

		val messageType = Array[Byte](1)
		val encodedVersion = RBACCodec.Encoder.encode(Array[Byte](4))
		val fromSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedSrcAddress, encodedSrcPort, encodedSrcPort))
		val toSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedDestAddress, encodedDestPort, encodedDestPort))

		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedVersion, fromSeq, toSeq, encodedExpiration))

		val result: PingMessage = TransportMessage.encode(messageType, data, privateKey)
		result._expiration = expiration
		result._address = srcAddress.getAddress
		result._port = srcAddress.getPort
		result
	}

}
