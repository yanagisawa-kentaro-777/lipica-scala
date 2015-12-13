package org.lipicalabs.lipica.core.net.transport.discover

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 12:58
 * YANAGISAWA, Kentaro
 */
class NodeManager {
	//TODO 未実装。

	def getBestLpcNodes(used: Set[String], lowerDifficulty: BigInt, limit: Int): Seq[NodeHandler] = ???

	def addDiscoveryListener(listener: DiscoverListener, predicate: (NodeStatistics) => Boolean): Unit = ???
}
