package org.lipicalabs.lipica.core.net.transport

import java.net.{InetAddress, URI}

import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult

/**
 * Created by IntelliJ IDEA.
 * 2015/12/11 19:38
 * YANAGISAWA, Kentaro
 */
class Node(private var _id: ImmutableBytes, val address: InetAddress, private var _port: Int) extends Serializable {

	def id: ImmutableBytes = this._id
	def id_=(v: ImmutableBytes): Unit = this._id = v
	def hexId: String = this.id.toHexString
	def hexIdShort: String = this.hexId.substring(0, 8)

//	def host: String = this._host
//	def host_=(v: String): Unit = this._host = v

	def port: Int = this._port
	def port_=(v: Int): Unit = this._port = v

	def toEncodedBytes: ImmutableBytes = {
		val encodedHost = RBACCodec.Encoder.encode(ImmutableBytes(this.address.getAddress))
		val encodedUDPPort = RBACCodec.Encoder.encode(this.port)
		val encodedTCPPort = RBACCodec.Encoder.encode(this.port)
		val encodedId = RBACCodec.Encoder.encode(this.id)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHost, encodedUDPPort, encodedTCPPort, encodedId))
	}

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[Node]
			if (this eq another) {
				return true
			}
			this.id == another.id
		} catch {
			case any: Throwable =>
				false
		}
	}

	override def hashCode: Int = toString.hashCode

	override def toString: String = "Node[Id=%s..., Host=%s, Port=%d]".format( this.hexIdShort, this.address, this.port)

}

object Node {

	def apply(nodeURI: URI): Node = {
		try {
			val id = ImmutableBytes.parseHexString(nodeURI.getUserInfo)
			val address = InetAddress.getByName(nodeURI.getHost)
			val port = nodeURI.getPort
			new Node(id, address, port)
		} catch {
			case e: Throwable => throw e
		}
	}

	def decode(decodedResult: DecodedResult): Node = {
		val items = decodedResult.items
		val address =
			if ((items.head.bytes.length == 4) || (items.head.bytes.length == 16)) {
				InetAddress.getByAddress(items.head.bytes.toByteArray)
			} else {
				InetAddress.getByName(items.head.asString)
			}

		if (3 < items.length) {
			val udpPort = items(1).asInt
			val tcpPort = items(2).asInt
			val id = items(3).bytes
			new Node(id, address, udpPort)
		} else {
			val port = items(1).asInt
			val id = items(2).bytes
			new Node(id, address, port)
		}
	}

}