package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.util
import java.util.{Collections, Comparator}
import java.util.concurrent._

import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.peer_discovery.NodeHandler
import org.lipicalabs.lipica.core.utils.{ErrorLogger, UtilConsts}
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}

/**
 * ピア候補のノードと、正常に通信ができるか否かを検証するためのクラスです。
 * （自ノード全体で１個のインスタンス。）
 *
 * Created by IntelliJ IDEA.
 * 2015/12/22 12:21
 * YANAGISAWA, Kentaro
 */
class PeerConnectionExaminer {
	import PeerConnectionExaminer._

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private val connectedCandidates: mutable.Map[NodeHandler, NodeHandler] = JavaConversions.mapAsScalaMap(new util.IdentityHashMap[NodeHandler, NodeHandler])

	private val queue = new MutablePriorityQueue[Runnable, ConnectTask](new Comparator[ConnectTask] {
		override def compare(o1: ConnectTask, o2: ConnectTask): Int = o2.nodeHandler.nodeStatistics.reputation - o1.nodeHandler.nodeStatistics.reputation
	})
	private val peerConnectionPool = ExecutorPool.instance.peerConnectionExaminer(ConnectThreads, queue)

	private val reconnectTimer = ExecutorPool.instance.reconnectTimer
	private var _reconnectPeersCount = 0

	class ConnectTask(val nodeHandler: NodeHandler) extends Runnable {
		override def run(): Unit = {
			try {
				this.nodeHandler.nodeStatistics.transportConnectionAttempts.add
				if (logger.isDebugEnabled) {
					logger.debug("<PeerConnectionExaminer> Trying node connection " + this.nodeHandler)
				}
				//
				//接続を試行し、切断されるまでブロックする。
				val node = this.nodeHandler.node
				componentsMotherboard.client.connect(node.address, node.id, discoveryMode = true)
				if (logger.isDebugEnabled) {
					logger.debug("<PeerConnectionExaminer> Terminated node connection " + this.nodeHandler)
				}
				//ここに来たということは、切断された。
				this.nodeHandler.nodeStatistics.disconnected()
				if ((this.nodeHandler.nodeStatistics.lpcTotalDifficulty != UtilConsts.Zero) && (0 < ReconnectPeriod) && ((_reconnectPeersCount < ReconnectMaxPeers) || (ReconnectMaxPeers < 0))) {
					//これは悪くないノードであるから、定期的にタッチを更新する。
					_reconnectPeersCount += 1
					reconnectTimer.schedule(new Runnable {
						override def run() = {
							peerConnectionPool.execute(new ConnectTask(nodeHandler))
							_reconnectPeersCount -= 1
						}
					}, ReconnectPeriod, TimeUnit.MILLISECONDS)
					if (logger.isDebugEnabled) {
						logger.debug("<PeerConnectionExaminer> Keeping in touch with %s".format(this.nodeHandler))
					}
				} else {
					if (logger.isDebugEnabled) {
						logger.debug("<PeerConnectionExaminer> Forgetting %s".format(this.nodeHandler))
					}
				}
			} catch {
				case e: Exception =>
					ErrorLogger.logger.warn("<PeerConnectionExaminer> Exception caught: %s".format(e.getClass.getSimpleName), e)
					logger.warn("<PeerConnectionExaminer> Exception caught: %s".format(e.getClass.getSimpleName), e)
			}
		}
	}

	def nodeStatusChanged(nodeHandler: NodeHandler): Unit = {
		if (!connectedCandidates.contains(nodeHandler)) {
			logger.debug("<PeerConnectionExaminer> Submitting node for transport: " + nodeHandler)
			this.connectedCandidates.put(nodeHandler, nodeHandler)
			this.peerConnectionPool.execute(new ConnectTask(nodeHandler))
		}
	}

}

object PeerConnectionExaminer {
	private val logger = LoggerFactory.getLogger("discovery")

	private val ConnectThreads = NodeProperties.CONFIG.peerDiscoveryWorkers
	private val ReconnectPeriod = NodeProperties.CONFIG.peerDiscoveryTouchSeconds * 1000L
	private val ReconnectMaxPeers = NodeProperties.CONFIG.peerDiscoveryTouchMaxNodes


	class MutablePriorityQueue[T, C <: T](val comparator: Comparator[C]) extends LinkedBlockingDeque[T] {

		private def privateSelectFirst: T = {
			Collections.min(this, comparator.asInstanceOf[Comparator[T]])
		}

		private def privateRemoveFirst: T = {
			val result = privateSelectFirst
			remove(result)
			result
		}

		override def take: T = {
			if (isEmpty) {
				super.take
			} else {
				privateRemoveFirst
			}
		}

		override def poll(timeout: Long, unit: TimeUnit): T = {
			if (isEmpty) {
				super.poll(timeout, unit)
			} else {
				privateRemoveFirst
			}
		}

		override def peek: T = {
			if (isEmpty) {
				super.peek
			} else {
				privateSelectFirst
			}
		}
	}

}