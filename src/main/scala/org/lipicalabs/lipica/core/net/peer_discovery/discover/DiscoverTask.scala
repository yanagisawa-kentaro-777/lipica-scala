package org.lipicalabs.lipica.core.net.peer_discovery.discover

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeManager, Node}
import org.lipicalabs.lipica.core.net.peer_discovery.discover.table.KademliaOptions
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks

/**
 * 近傍のノードに FindNode メッセージを送信して、
 * ピアの情報を収集するタスクです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:09
 * YANAGISAWA, Kentaro
 */
class DiscoverTask(val nodeManager: NodeManager) extends Runnable {
	import DiscoverTask._

	/**
	 * 自ノードのノードID。
	 */
	private val nodeId: NodeId = nodeManager.homeNode.id

	override def run(): Unit = {
		discover(this.nodeId, 0, Seq.empty)
	}

	/**
	 * 近傍のノードに対して FindNode メッセージを送信し、
	 * さらに近傍ノードの情報を収集します。
	 */
	def discover(nodeId: NodeId, round: Int, prevTried: Seq[Node]): Unit = {
		try {
			if (round == KademliaOptions.MaxSteps) {
				//今回のラウンドは、十分に情報を収集した。
				logger.info("<DiscoverTask> %,d nodes found.".format(this.nodeManager.table.nodeCount))
				return
			}

			//自ノードの近傍のノードを取得する。
			val closest = this.nodeManager.table.closedNodes(nodeId)
			val tried: mutable.Buffer[Node] = new ArrayBuffer[Node]

			val brk = new Breaks
			brk.breakable {
				for (n <- closest) {
					if (!tried.contains(n) && !prevTried.contains(n)) {
						//近傍のノードの情報を要求する。
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
				//情報がまったく収集できなかった。継続しても意義が希薄だと思われるので、中断する。
				logger.info("<DiscoverTask> Tried empty. Terminating task after %,d rounds.".format(round))
				return
			}
			tried.appendAll(prevTried)
			//再度情報収集を実行する。
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