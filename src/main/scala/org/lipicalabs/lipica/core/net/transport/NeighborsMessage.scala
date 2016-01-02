package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 19:44
 * YANAGISAWA, Kentaro
 */
class NeighborsMessage extends TransportMessage {

	private var _nodes: Seq[Node] = null
	def nodes: Seq[Node] = this._nodes

	private var _timestamp: Long = System.currentTimeMillis / 1000L
	def timestamp: Long = this._timestamp

	override def parse(data: Array[Byte]): Unit = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		val encodedNodes = items.head.items
		this._nodes = encodedNodes.map(each => Node.decode(each))
		this._timestamp = items(1).asPositiveLong
	}

	override def toString: String = {
		"[NeighborsMessage] nodes=%,d, timestamp=%,d. %s".format(
			this.nodes.size, this.timestamp, super.toString
		)
	}

}

object NeighborsMessage {

	def create(nodes: Seq[Node], privateKey: ECKey): NeighborsMessage = {
		val timestamp = System.currentTimeMillis / 1000L

		val encodedNodes = nodes.map(_.toEncodedBytes)
		val encodedNodesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(encodedNodes)
		val encodedTimestamp = RBACCodec.Encoder.encode(ByteUtils.toByteArrayWithNoLeadingZeros(timestamp))

		val messageType = Array[Byte](4)
		val data = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedNodesSeq, encodedTimestamp))

		val result: NeighborsMessage = TransportMessage.encode(messageType, data, privateKey)
		result._nodes = nodes
		result._timestamp = timestamp
		result
	}

}