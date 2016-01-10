package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.{InetAddress, InetSocketAddress, URI}

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.ImmutableBytes

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
class Node(val id: ImmutableBytes, val address: InetSocketAddress) extends Serializable {

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
			val id = ImmutableBytes.parseHexString(nodeURI.getUserInfo)
			val address = InetAddress.getByName(nodeURI.getHost)
			val port = nodeURI.getPort
			val socketAddress = new InetSocketAddress(address, port)
			new Node(id, socketAddress)
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
			val socketAddress = new InetSocketAddress(address, udpPort)
			new Node(id, socketAddress)
		} else {
			val port = items(1).asInt
			val id = items(2).bytes
			val socketAddress = new InetSocketAddress(address, port)
			new Node(id, socketAddress)
		}
	}

}