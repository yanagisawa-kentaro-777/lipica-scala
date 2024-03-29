package org.lipicalabs.lipica.core.net.peer_discovery.discover

import org.lipicalabs.lipica.core.net.peer_discovery.NodeHandler

/**
 * ノードディスカバリーに関する事象の発生を検知するリスナークラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/13 14:10
 * YANAGISAWA, Kentaro
 */
trait DiscoverListener {

	def nodeAppeared(handler: NodeHandler): Unit

	def nodeDisappeared(handler: NodeHandler): Unit

}
