package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.util.Random

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeManager}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:26
 * YANAGISAWA, Kentaro
 */
class RefreshTask(_nodeManager: NodeManager) extends DiscoverTask(_nodeManager) {

	private def getNodeId: NodeId = {
		val random = new Random
		NodeId(ImmutableBytes.createRandom(random, 64))
	}

	override def run(): Unit = discover(getNodeId, 0, Seq.empty)

}
