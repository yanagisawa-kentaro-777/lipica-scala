package org.lipicalabs.lipica.core.net.transport

import java.net.InetAddress

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec}

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

	private var _timestamp: Long = System.currentTimeMillis / 1000L
	def timestamp: Long = this._timestamp

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		val fromSeq = items(1).items
		this._address = InetAddress.getByAddress(fromSeq.head.bytes.toByteArray)
		this._port = fromSeq(1).asInt
		this._timestamp = items(3).asPositiveLong
	}

	override def toString: String = {
		"[PingMessage] Address=%s; Port=%d; Timestamp=%,d. %s".format(
			this.address, this.port, this.timestamp, super.toString
		)
	}
}

object PingMessage {

	def create(address: InetAddress, port: Int, privateKey: ECKey): PingMessage = {
		val timestamp = System.currentTimeMillis / 1000L

		val addressBytes = address.getAddress
		val encodedAddress = RBACCodec.Encoder.encode(addressBytes)
		val portBytes = ByteUtils.toByteArrayWithNoLeadingZeros(port)
		val encodedPort = RBACCodec.Encoder.encode(portBytes)

		val encodedHostTo = RBACCodec.Encoder.encode(addressBytes)
		val encodedPortTo = RBACCodec.Encoder.encode(portBytes)

		val timestampBytes = ByteUtils.toByteArrayWithNoLeadingZeros(timestamp)
		val encodedTimestamp = RBACCodec.Encoder.encode(timestampBytes)

		val messageType = Array[Byte](1)
		val encodedVersion = RBACCodec.Encoder.encode(Array[Byte](4))
		val fromSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedPort))
		val toSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHostTo, encodedPortTo, encodedPortTo))

		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedVersion, fromSeq, toSeq, encodedTimestamp))

		val result: PingMessage = TransportMessage.encode(messageType, data, privateKey)
		result._timestamp = timestamp
		result._address = address
		result._port = port
		result
	}

}
