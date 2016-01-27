package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.security.SecureRandom
import java.util.Random

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeManager}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ランダムな位置の近傍にあるノードの情報を収集するタスク。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:26
 * YANAGISAWA, Kentaro
 */
class RefreshTask(_nodeManager: NodeManager) extends DiscoverTask(_nodeManager) {

	private def generateRandomNodeId: NodeId = {
		val random = new SecureRandom
		NodeId(ImmutableBytes.createRandom(random, NodeId.NumBytes))
	}

	override def run(): Unit = discover(generateRandomNodeId, 0, Seq.empty)

}
