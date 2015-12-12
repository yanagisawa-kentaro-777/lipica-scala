package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Peers
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 13:14
 * YANAGISAWA, Kentaro
 */
case class PeersMessage(peers: Set[Peer]) extends P2PMessage {

	override def toEncodedBytes = {
		val seqOfBytes = this.peers.toSeq.map(_.toEncodedBytes)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seqOfBytes)
	}

	override def code = Peers.asByte

}

object PeersMessage {

	def decode(encodedBytes: ImmutableBytes): PeersMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val peers = items.map(each => Peer.decode(each.items)).toSet
		PeersMessage(peers)
	}

}