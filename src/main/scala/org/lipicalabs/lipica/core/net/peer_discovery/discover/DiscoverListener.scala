package org.lipicalabs.lipica.core.net.peer_discovery.discover

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 14:10
 * YANAGISAWA, Kentaro
 */
trait DiscoverListener {

	def nodeAppeared(handler: NodeHandler): Unit

	def nodeDisappeared(handler: NodeHandler): Unit

}
