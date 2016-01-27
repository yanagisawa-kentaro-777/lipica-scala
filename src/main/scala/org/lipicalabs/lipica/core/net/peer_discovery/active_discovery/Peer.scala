package org.lipicalabs.lipica.core.net.peer_discovery.active_discovery

import java.net.InetAddress

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.ImmutableBytes


/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:37
 * YANAGISAWA, Kentaro
 */
case class Peer(address: InetAddress, port: Int, nodeId: NodeId, capabilities: Seq[Capability]) {

	def toEncodedBytes: ImmutableBytes = {
		val encodedAddress = RBACCodec.Encoder.encode(this.address.getAddress)
		val encodedPort = RBACCodec.Encoder.encode(this.port)
		val encodedNodeId = RBACCodec.Encoder.encode(this.nodeId.toHexString)
		val encodedCapabilitiesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.capabilities.toSeq.map(_.toEncodedBytes))
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedNodeId, encodedCapabilitiesSeq))
	}


	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[Peer]
			(this.nodeId == another.nodeId) ||
				((this.address == another.address) && (this.port == another.port))
		} catch {
			case any: Throwable => false
		}
	}

	override def hashCode: Int = toString.hashCode

	override def toString: String = "Address=%s; Port=%d; NodeId=%s".format(this.address, this.port, this.nodeId)

}

object Peer {
	def decode(encodedBytes: ImmutableBytes): Peer = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		decode(items)
	}

	def decode(items: Seq[DecodedResult]): Peer = {
		val address = InetAddress.getByAddress(items.head.bytes.toByteArray)
		val port = items(1).asInt
		val nodeId = items(2).asString
		val capabilities = items(3).items.map(each => Capability.decode(each.items))
		new Peer(address, port, NodeId.parseHexString(nodeId), capabilities)
	}
}