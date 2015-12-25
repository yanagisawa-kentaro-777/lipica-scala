package org.lipicalabs.lipica.core.net.transport.discover

import java.util
import java.util.{Collections, Comparator}
import java.util.concurrent.{Executors, LinkedBlockingDeque, TimeUnit, ThreadPoolExecutor}

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.utils.UtilConsts
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/22 12:21
 * YANAGISAWA, Kentaro
 */
class PeerConnectionManager {
	import PeerConnectionManager._

	private def worldManager: WorldManager = WorldManager.instance

	private val connectedCandidates: mutable.Map[NodeHandler, NodeHandler] = JavaConversions.mapAsScalaMap(new util.IdentityHashMap[NodeHandler, NodeHandler])

	private val peerConnectionPool = new ThreadPoolExecutor(ConnectThreads, ConnectThreads, 0L, TimeUnit.SECONDS, new MutablePriorityQueue[Runnable, ConnectTask](new Comparator[ConnectTask] {
		override def compare(o1: ConnectTask, o2: ConnectTask): Int = o2.nodeHandler.nodeStatistics.reputation - o1.nodeHandler.nodeStatistics.reputation
	}))

	private val reconnectTimer = Executors.newSingleThreadScheduledExecutor
	private var _reconnectPeersCount = 0

	class ConnectTask(val nodeHandler: NodeHandler) extends Runnable {
		override def run(): Unit = {
			try {
				this.nodeHandler.nodeStatistics.transportConnectionAttempts.add
				if (logger.isDebugEnabled) {
					logger.debug("<PeerConnectionManager> Trying node connection " + this.nodeHandler)
				}
				val node = this.nodeHandler.node
				worldManager.activePeer.connect(node.host, node.port, node.id.toHexString, discoveryMode = true)
				if (logger.isDebugEnabled) {
					logger.debug("<PeerConnectionManager> Terminated node connection " + this.nodeHandler)
				}
				this.nodeHandler.nodeStatistics.disconnected()
				if ((this.nodeHandler.nodeStatistics.lpcTotalDifficulty != UtilConsts.Zero) && (0 < ReconnectPeriod) && ((_reconnectPeersCount < ReconnectMaxPeers) || (ReconnectMaxPeers < 0))) {
					_reconnectPeersCount += 1
					reconnectTimer.schedule(new Runnable {
						override def run() = {
							peerConnectionPool.execute(new ConnectTask(nodeHandler))
							_reconnectPeersCount -= 1
						}
					}, ReconnectPeriod, TimeUnit.MILLISECONDS)
				}
			} catch {
				case e: Exception =>
					logger.warn("<PeerConnectionManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
			}
		}
	}

	def nodeStatusChanged(nodeHandler: NodeHandler): Unit = {
		if (!connectedCandidates.contains(nodeHandler)) {
			logger.debug("<PeerConnectionManager> Submitting node for transport: " + nodeHandler)
		}
		this.connectedCandidates.put(nodeHandler, nodeHandler)
		this.peerConnectionPool.execute(new ConnectTask(nodeHandler))
	}

}

object PeerConnectionManager {
	private val logger = LoggerFactory.getLogger("discover")

	private val ConnectThreads = SystemProperties.CONFIG.peerDiscoveryWorkers
	private val ReconnectPeriod = SystemProperties.CONFIG.peerDiscoveryTouchSeconds * 1000L
	private val ReconnectMaxPeers = SystemProperties.CONFIG.peerDiscoveryTouchMaxNodes


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