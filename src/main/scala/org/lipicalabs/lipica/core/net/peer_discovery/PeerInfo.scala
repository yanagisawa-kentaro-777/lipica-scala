package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetAddress

import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.p2p.HelloMessage

import scala.StringBuilder
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:28
 * YANAGISAWA, Kentaro
 */
class PeerInfo(val address: InetAddress, val port: Int, val peerId: String) {

	@transient
	private var _online: Boolean = false
	def online: Boolean = {
		if (this.capabilities.isEmpty) {
			false
		} else {
			this._online
		}
	}
	def online_=(v: Boolean): Unit = this._online = v

	@transient
	private var _lastCheckTime: Long = 0L
	def lastCheckTime: Long = this._lastCheckTime
	def lastCheckTime_=(v: Long): Unit = this._lastCheckTime = v

	private val _capabilities: mutable.Buffer[Capability] = new ArrayBuffer[Capability]
	def addCapabilities(aCapabilities: Iterable[Capability]): Unit = this._capabilities.appendAll(aCapabilities)
	def capabilities: Seq[Capability] = this._capabilities.toSeq

	private var _handshakeHelloMessage: HelloMessage = null
	def handshakeHelloMessage: HelloMessage = this._handshakeHelloMessage
	def handshakeHelloMessage_=(v: HelloMessage): Unit = this._handshakeHelloMessage = v

	private var _statusMessage: StatusMessage = null
	def statusMessage: StatusMessage = this._statusMessage
	def statusMessage_=(v: StatusMessage): Unit = this._statusMessage = v

	override def hashCode: Int = {
		this.address.hashCode * 31 + this.port
	}

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[PeerInfo]
			(this.address == another.address) && (this.port == another.port)
		} catch {
			case any: Throwable => false
		}
	}

	override def toString: String = {
		"PeerInfo[Address=%s, Port=%d, PeerId=%s, HelloMessage=%s, StatusMessage=%s".format(
			this.address, this.port, this.peerId, this.handshakeHelloMessage, this.statusMessage
		)
	}

}
