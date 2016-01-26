package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.net.InetSocketAddress

import org.lipicalabs.lipica.core.net.peer_discovery.message.AbstractPeerDiscoveryMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:22
 * YANAGISAWA, Kentaro
 */
class DiscoveryEvent(val message: AbstractPeerDiscoveryMessage, val address: InetSocketAddress) {
	//
}
