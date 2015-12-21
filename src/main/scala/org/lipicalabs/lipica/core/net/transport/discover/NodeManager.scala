package org.lipicalabs.lipica.core.net.transport.discover

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.net.transport.discover.table.NodeTable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 12:58
 * YANAGISAWA, Kentaro
 */
class NodeManager {
	//TODO 未実装。

	def key: ECKey = ???

	def homeNode: Node = ???

	def table: NodeTable = ???

	def channelActivated(): Unit = ???

	def handleInbound(event: DiscoveryEvent): Unit = ???

	def getNodeHandler(n: Node): NodeHandler = ???

	def getBestLpcNodes(used: Set[String], lowerDifficulty: BigInt, limit: Int): Seq[NodeHandler] = ???

	def addDiscoveryListener(listener: DiscoverListener, predicate: (NodeStatistics) => Boolean): Unit = ???

	def stateChanged(nodeHandler: NodeHandler, oldState: NodeHandler.State, newState: NodeHandler.State): Unit = ???

	def sendOutbound(event: DiscoveryEvent): Unit = ???

}
