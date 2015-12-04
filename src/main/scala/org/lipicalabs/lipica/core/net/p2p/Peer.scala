package org.lipicalabs.lipica.core.net.p2p

import java.net.InetAddress

import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:37
 * YANAGISAWA, Kentaro
 */
class Peer(val address: InetAddress, val port: Int, val peerId: String) {

	private val capabilities: mutable.Buffer[Capability] = new ArrayBuffer[Capability]

	def encode: ImmutableBytes = {
		val encodedAddress = RBACCodec.Encoder.encode(this.address.getAddress)
		val encodedPort = RBACCodec.Encoder.encode(this.port)
		val encodedPeerId = RBACCodec.Encoder.encode(this.peerId)
		val encodedCapabilitiesSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.capabilities.toSeq.map(_.toEncodedBytes))
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedPort, encodedPeerId, encodedCapabilitiesSeq))
	}


	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[Peer]
			(this.peerId == another.peerId) ||
				((this.address == another.address) && (this.port == another.port))
		} catch {
			case any: Throwable => false
		}
	}

	override def hashCode: Int = toString.hashCode

	override def toString: String = "Address=%s; Port=%d; PeerId=%s".format(this.address, this.port, this.peerId)

}
