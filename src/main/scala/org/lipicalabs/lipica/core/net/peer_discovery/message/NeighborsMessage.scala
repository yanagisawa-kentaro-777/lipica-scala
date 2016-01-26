package org.lipicalabs.lipica.core.net.peer_discovery.message

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.peer_discovery.Node
import org.lipicalabs.lipica.core.utils.ByteUtils

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 19:44
 * YANAGISAWA, Kentaro
 */
class NeighborsMessage extends AbstractPeerDiscoveryMessage {

	private var _nodes: Seq[Node] = null
	def nodes: Seq[Node] = this._nodes

	private var _expiration: Long = (System.currentTimeMillis / 1000L) + 60L
	def expiration: Long = this._expiration

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		val encodedNodes = items.head.items
		this._nodes = encodedNodes.map(each => Node.decode(each))
		this._expiration = items(1).asPositiveLong
	}

	override def toString: String = {
		"[NeighborsMessage] nodes=%,d, expiration=%,d. %s".format(
			this.nodes.size, this.expiration, super.toString
		)
	}

}

object NeighborsMessage {

	def create(nodes: Seq[Node], privateKey: ECKey): NeighborsMessage = {
		val expiration = (System.currentTimeMillis / 1000L) + 60L

		val encodedNodes = nodes.map(_.toEncodedBytes)
		val encodedNodesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(encodedNodes)
		val encodedExpiration = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(expiration))

		val messageType = Array[Byte](4)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedNodesSeq, encodedExpiration))

		val result: NeighborsMessage = AbstractPeerDiscoveryMessage.encode(messageType, data, privateKey)
		result._nodes = nodes
		result._expiration = expiration
		result
	}

}