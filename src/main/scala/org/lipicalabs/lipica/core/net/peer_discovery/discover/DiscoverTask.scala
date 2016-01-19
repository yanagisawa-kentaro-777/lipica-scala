package org.lipicalabs.lipica.core.net.peer_discovery.discover

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeManager, Node}
import org.lipicalabs.lipica.core.net.peer_discovery.discover.table.KademliaOptions
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks

/**
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:09
 * YANAGISAWA, Kentaro
 */
class DiscoverTask(val nodeManager: NodeManager) extends Runnable {
	import DiscoverTask._

	private val nodeId: NodeId = nodeManager.homeNode.id

	override def run(): Unit = {
		discover(this.nodeId, 0, Seq.empty)
	}

	def discover(nodeId: NodeId, round: Int, prevTried: Seq[Node]): Unit = {
		try {
			if (round == KademliaOptions.MaxSteps) {
				logger.info("<DiscoverTask> %,d nodes found.".format(this.nodeManager.table.getNodeCount))
				return
			}
			//val total = this.nodeManager.table.getNodeCount
			val closest = this.nodeManager.table.getClosestNodes(nodeId)

			val tried: mutable.Buffer[Node] = new ArrayBuffer[Node]

			val brk = new Breaks
			brk.breakable {
				for (n <- closest) {
					if (!tried.contains(n) && !prevTried.contains(n)) {
						this.nodeManager.getNodeHandler(n).sendFindNode(nodeId)
						tried.append(n)
						Thread.sleep(50L)
					}
					if (KademliaOptions.Alpha <= tried.size) {
						brk.break()
					}
				}
			}
			if (tried.isEmpty) {
				logger.info("<DiscoverTask> Tried empty. Terminating task after %,d rounds.".format(round))
				return
			}
			tried.appendAll(prevTried)

			discover(nodeId, round + 1, tried.toSeq)
		} catch {
			case e: Throwable =>
				logger.info("<DiscoverTask> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}

}

object DiscoverTask {
	private val logger = LoggerFactory.getLogger("discovery")
}