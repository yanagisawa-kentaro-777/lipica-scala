package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.{InetAddress, InetSocketAddress, URI}

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.ImmutableBytes

class NodeId private (val bytes: ImmutableBytes) extends Comparable[NodeId] {

	def toByteArray: Array[Byte] = this.bytes.toByteArray

	/**
	 * アドレスのバイト数。
	 */
	def length: Int = this.bytes.length

	def isEmpty: Boolean = this.length == 0

	override def hashCode: Int = this.bytes.hashCode

	override def equals(o: Any): Boolean = {
		try {
			this.bytes == o.asInstanceOf[NodeId].bytes
		} catch {
			case any: Throwable => false
		}
	}

	override def compareTo(o: NodeId): Int = {
		this.bytes.compareTo(o.bytes)
	}

	def toShortString: String = this.bytes.toShortString

	def toHexString: String = this.bytes.toHexString

	override def toString: String = this.toHexString
}

object NodeId {

	val empty: NodeId = new NodeId(ImmutableBytes.empty)

	val NumBytes = 64

	def apply(bytes: ImmutableBytes): NodeId = {
		new NodeId(bytes)
	}

	def apply(bytes: Array[Byte]): NodeId = {
		new NodeId(ImmutableBytes(bytes))
	}

	def parseHexString(s: String): NodeId = {
		new NodeId(ImmutableBytes.parseHexString(s))
	}
}

/**
 * 自ノードもしくは他ノードの中核的な情報をモデル化したクラスです。
 *
 * @param id ノードの一意識別子。（内容はノードの秘密鍵に対応する公開鍵です。）
 * @param address ノードのアドレスおよびポート番号。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/11 19:38
 * YANAGISAWA, Kentaro
 */
class Node(val id: NodeId, val address: InetSocketAddress) extends Serializable {

	def toEncodedBytes: ImmutableBytes = {
		val encodedAddress = RBACCodec.Encoder.encode(ImmutableBytes(this.address.getAddress.getAddress))
		val port = this.address.getPort
		val encodedUDPPort = RBACCodec.Encoder.encode(port)
		val encodedTCPPort = RBACCodec.Encoder.encode(port)
		val encodedId = RBACCodec.Encoder.encode(this.id)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedUDPPort, encodedTCPPort, encodedId))
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

	override def toString: String = "Node[Id=%s; Address=%s]".format( this.id.toShortString, this.address)

}

object Node {

	def apply(nodeURI: URI): Node = {
		try {
			val id = NodeId(ImmutableBytes.parseHexString(nodeURI.getUserInfo))
			val address = InetAddress.getByName(nodeURI.getHost)
			val port = nodeURI.getPort
			val socketAddress = new InetSocketAddress(address, port)
			new Node(id, socketAddress)
		} catch {
			case e: Throwable => throw e
		}
	}

	def decode(encodedBytes: ImmutableBytes): Node = {
		val decodedResult = RBACCodec.Decoder.decode(encodedBytes).right.get
		decode(decodedResult)
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
			val id = NodeId(items(3).bytes)
			val socketAddress = new InetSocketAddress(address, udpPort)
			new Node(id, socketAddress)
		} else {
			val port = items(1).asInt
			val id = NodeId(items(2).bytes)
			val socketAddress = new InetSocketAddress(address, port)
			new Node(id, socketAddress)
		}
	}

}