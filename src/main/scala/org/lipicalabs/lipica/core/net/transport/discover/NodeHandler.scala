package org.lipicalabs.lipica.core.net.transport.discover

import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 13:25
 * YANAGISAWA, Kentaro
 */
class NodeHandler {
	//TODO 未実装
	def getNode: Node = ???
	def getNodeStatistics: NodeStatistics = ???

	def sendFindNode(target: ImmutableBytes): Unit = ???
}
