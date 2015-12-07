package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetAddress

import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.p2p.HelloMessage

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:28
 * YANAGISAWA, Kentaro
 */
class PeerInfo(val address: InetAddress, val port: Int, val peerId: String) {

	private val capabilities: mutable.Buffer[Capability] = new ArrayBuffer[Capability]

	private var handshakeHelloMessage: HelloMessage = null

	//TODO 未実装。

}
