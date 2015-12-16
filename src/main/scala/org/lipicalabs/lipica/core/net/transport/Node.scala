package org.lipicalabs.lipica.core.net.transport

import java.net.URI
import java.nio.charset.StandardCharsets

import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult

/**
 * Created by IntelliJ IDEA.
 * 2015/12/11 19:38
 * YANAGISAWA, Kentaro
 */
class Node(private var _id: ImmutableBytes, private var _host: String, private var _port: Int) extends Serializable {

	def id: ImmutableBytes = this._id
	def id_=(v: ImmutableBytes): Unit = this._id = v
	def hexId: String = this.id.toHexString
	def hexIdShort: String = this.hexId.substring(0, 8)

	def host: String = this._host
	def host_=(v: String): Unit = this._host = v

	def port: Int = this._port
	def port_=(v: Int): Unit = this._port = v

	def toEncodedBytes: ImmutableBytes = {
		val encodedHost = RBACCodec.Encoder.encode(this.host.getBytes(StandardCharsets.UTF_8))
		val encodedTCPPort = RBACCodec.Encoder.encode(this.port)
		val encodedUDPPort = RBACCodec.Encoder.encode(this.port)
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

	override def toString: String = "Node{host=%s, port=%d, id=%s}".format(this.host, this.port, this.id)

}

object Node {

	def apply(enodeURI: URI): Node = {
		try {
			if (enodeURI.getScheme != "enode") {
				throw new IllegalArgumentException("enode://PUBKEY@HOST:PORT")
			}
			val id = ImmutableBytes.parseHexString(enodeURI.getUserInfo)
			val host = enodeURI.getHost
			val port = enodeURI.getPort
			new Node(id, host, port)
		} catch {
			case e: Throwable => throw e
		}
	}

	def decode(decodedResult: DecodedResult): Node = {
		val items = decodedResult.items
		val host = items.head.asString
		val port = items(1).asInt
		val id = items.last.bytes
		new Node(id, host, port)
	}

}