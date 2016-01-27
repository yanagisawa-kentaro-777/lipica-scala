package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.net.InetSocketAddress

import org.lipicalabs.lipica.core.net.peer_discovery.message.AbstractPeerDiscoveryMessage

/**
 * ピアディスカバリーに関するメッセージの授受発生を
 * 表現するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:22
 * YANAGISAWA, Kentaro
 */
class DiscoveryEvent(val message: AbstractPeerDiscoveryMessage, val address: InetSocketAddress) {
	//
}
