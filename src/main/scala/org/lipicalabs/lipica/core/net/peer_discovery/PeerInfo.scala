package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetSocketAddress
import java.util.concurrent.atomic.{AtomicReference, AtomicLong, AtomicBoolean}

import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.p2p.HelloMessage

import scala.collection.mutable


/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:28
 * YANAGISAWA, Kentaro
 */
class PeerInfo(val address: InetSocketAddress, val nodeId: NodeId) {

	private val onlineRef: AtomicBoolean = new AtomicBoolean(false)
	def online: Boolean = {
		if (this.capabilities.isEmpty) {
			false
		} else {
			this.onlineRef.get
		}
	}
	def online_=(v: Boolean): Unit = this.onlineRef.set(v)

	private val lastCheckTimeRef: AtomicLong = new AtomicLong(0L)
	def lastCheckTime: Long = this.lastCheckTimeRef.get
	def lastCheckTime_=(v: Long): Unit = this.lastCheckTimeRef.set(v)

	private val capabilitiesRef: AtomicReference[Iterable[Capability]] = new AtomicReference[Iterable[Capability]](mutable.Iterable.empty)
	def addCapabilities(aCapabilities: Iterable[Capability]): Unit = {
		this.capabilitiesRef.synchronized {
			val current = this.capabilitiesRef.get
			this.capabilitiesRef.set(current ++ aCapabilities)
		}
	}
	def capabilities: Seq[Capability] = this.capabilitiesRef.get.toSeq

	private val handshakeHelloMessageRef: AtomicReference[HelloMessage] = new AtomicReference[HelloMessage](null)
	def handshakeHelloMessage: HelloMessage = this.handshakeHelloMessageRef.get
	def handshakeHelloMessage_=(v: HelloMessage): Unit = this.handshakeHelloMessageRef.set(v)
	def handshakeHelloMessageOption: Option[HelloMessage] = Option(this.handshakeHelloMessageRef.get)

	private val statusMessageRef: AtomicReference[StatusMessage] = new AtomicReference[StatusMessage](null)
	def statusMessage: StatusMessage = this.statusMessageRef.get
	def statusMessage_=(v: StatusMessage): Unit = this.statusMessageRef.set(v)
	def statusMessageOption: Option[StatusMessage] = Option(this.statusMessageRef.get)

	override def hashCode: Int = {
		//nodeIdは、敢えて同一性の基準に含めない。
		this.address.getAddress.hashCode * 31 + this.address.getPort.hashCode
	}

	override def equals(o: Any): Boolean = {
		try {
			//nodeIdは、敢えて同一性の基準に含めない。
			//同一アドレス＆ポート間で後勝ち。
			val another = o.asInstanceOf[PeerInfo]
			this.address == another.address
		} catch {
			case any: Throwable => false
		}
	}

	override def toString: String = {
		"PeerInfo[Address=%s, NodeId=%s, HelloMessage=%s, StatusMessage=%s".format(
			this.address, this.nodeId.toShortString, this.handshakeHelloMessageOption, this.statusMessageOption
		)
	}

}
