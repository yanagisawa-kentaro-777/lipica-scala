package org.lipicalabs.lipica.core.net.lpc.sync

import java.util.concurrent.{Executors, TimeUnit}

import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.net.channel.Channel
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:24
 * YANAGISAWA, Kentaro
 */
class PeersPool {
	import PeersPool._

	private val activePeers: mutable.Map[ImmutableBytes, Channel] = new mutable.HashMap[ImmutableBytes, Channel]
	private val bannedPeers: mutable.Set[Channel] = new mutable.HashSet[Channel]
	private val disconnectHits: mutable.Map[String, Int] = new mutable.HashMap[String, Int]
	private val bans: mutable.Map[String, Long] = new mutable.HashMap[String, Long]
	private val pendingConnections: mutable.Map[String, Long] = new mutable.HashMap[String, Long]

	private def lipica: Lipica = Lipica.instance

	def init(): Unit = {
		Executors.newSingleThreadScheduledExecutor.scheduleWithFixedDelay(
			new Runnable {
				override def run(): Unit = {
					releaseBans()
					processConnections()
				}
			}, WorkerTimeoutSeconds, WorkerTimeoutSeconds, TimeUnit.SECONDS
		)
	}

	def add(peer: Channel): Unit = {
		this.activePeers.synchronized {
			this.activePeers.put(peer.nodeId, peer)
			this.bannedPeers.remove(peer)
		}
		this.pendingConnections.synchronized {
			this.pendingConnections.remove(peer.peerId)
		}
		this.bans.synchronized {
			this.bans.remove(peer.peerId)
		}
		logger.info("<PeersPool> %s. Added to pool.".format(peer.peerIdShort))
	}

	def remove(peer: Channel): Unit = {
		this.activePeers.synchronized {
			this.activePeers.remove(peer.nodeId)
		}
	}

	def removeAll(removed: Iterable[Channel]): Unit = {
		this.activePeers.synchronized {
			removed.foreach {
				each => this.activePeers.remove(each.nodeId)
			}
		}
	}

	def getBest: Option[Channel] = {
		this.activePeers.synchronized {
			if (this.activePeers.isEmpty) {
				return None
			}
			val best: (ImmutableBytes, Channel) = this.activePeers.reduceLeft {
				(accum, each) => if (accum._2.totalDifficulty < each._2.totalDifficulty) each else accum
			}
			Option(best._2)
		}
	}

	def getByNodeId(nodeId: ImmutableBytes): Option[Channel] = this.activePeers.get(nodeId)

	def onDisconnect(peer: Channel): Unit = {
		if (logger.isTraceEnabled) {
			logger.trace("<PeerPool> Peer %s: disconnected.".format(peer.peerIdShort))
		}
		if (peer.nodeId.isEmpty) {
			return
		}
		this.activePeers.synchronized {
			this.activePeers.remove(peer.nodeId)
			this.bannedPeers.remove(peer)
		}
		this.disconnectHits.synchronized {
			val hits = this.disconnectHits.getOrElse(peer.peerId, 0)
			if (DisconnectHitsThreshold < hits) {
				ban(peer)
				logger.info("<PeersPool> Peer %s is banned due to frequent disconnections.".format(peer.peerIdShort))
				this.disconnectHits.remove(peer.peerId)
			} else {
				this.disconnectHits.put(peer.peerId, hits + 1)
			}
		}
	}

	def connect(node: Node): Unit = {
		if (logger.isTraceEnabled) {
			logger.trace("<PeersPool> Peer %s: initiating connection.".format(node.hexIdShort))
		}
		if (isInUse(node.hexId)) {
			if (logger.isTraceEnabled) {
				logger.trace("<PeersPool> Peer %s: already initiated.".format(node.hexIdShort))
			}
			return
		}
		this.pendingConnections.synchronized {
			lipica.connect(node)
			this.pendingConnections.put(node.hexId, System.currentTimeMillis + ConnectionTimeout)
		}
	}


	def ban(peer: Channel): Unit = {
		println("BAN!")//TODO 20151229 DEBUG
		peer.changeSyncState(SyncStateName.Idle)
		this.activePeers.synchronized {
			if (this.activePeers.contains(peer.nodeId)) {
				this.activePeers.remove(peer.nodeId)
				this.bannedPeers.add(peer)
			}
		}
		this.bans.synchronized {
			this.bans.put(peer.peerId, System.currentTimeMillis + DefaultBanTimeout)
		}
	}

	def nodesInUse: Set[String] = {
		var result: Set[String] =
			this.activePeers.synchronized {
				this.activePeers.values.map(_.peerId).toSet
			}
		result ++= this.bans.synchronized(this.bans.keySet)
		result ++= this.pendingConnections.synchronized(this.pendingConnections.keySet)

		result
	}

	def isInUse(nodeId: String): Boolean = nodesInUse.contains(nodeId)

	def changeState(stateName: SyncStateName): Unit = {
		this.activePeers.synchronized {
			for (peer <- this.activePeers.values) {
				peer.changeSyncState(stateName)
			}
		}
	}

	def changeState(stateName: SyncStateName, predicate: (Channel) => Boolean): Unit = {
		this.activePeers.synchronized {
			this.activePeers.values.withFilter(each => predicate(each)).foreach {
				each => each.changeSyncState(stateName)
			}
		}
	}

	def isEmpty: Boolean = this.activePeers.isEmpty

	def activeCount: Int = this.activePeers.size

	def peers: Iterable[Channel] = {
		this.activePeers.synchronized {
			this.activePeers.values
		}
	}

	def logActivePeers(): Unit = {
		if (this.activePeers.nonEmpty) {
			logger.info("ActivePeers")
			logger.info("==========")
			for (peer <- peers) {
				peer.logSyncStats()
			}
		}
	}

	def logBannedPeers(): Unit = {
		//TODO 未実装。
	}

	private def releaseBans(): Unit = {
		var released: Set[String] = Set.empty
		this.bans.synchronized {
			released = getTimeoutExceeded(this.bans)
			this.activePeers.synchronized {
				for (peer <- this.bannedPeers) {
					if (released.contains(peer.peerId)) {
						this.activePeers.put(peer.nodeId, peer)
					}
				}
				this.activePeers.values.foreach(each => this.bannedPeers.remove(each))
			}
			released.foreach(each => this.bans.remove(each))
		}
		this.disconnectHits.synchronized {
			released.foreach(each => this.disconnectHits.remove(each))
		}
	}

	private def processConnections(): Unit = {
		this.pendingConnections.synchronized {
			getTimeoutExceeded(this.pendingConnections).foreach {
				each => this.pendingConnections.remove(each)
			}
		}
	}

	private def getTimeoutExceeded(map: mutable.Map[String, Long]): Set[String] = {
		val now = System.currentTimeMillis
		map.withFilter(entry => entry._2 <= now).map(entry => entry._1).toSet
	}

}

object PeersPool {

	private val logger = LoggerFactory.getLogger("sync")
	private val WorkerTimeoutSeconds = 3L
	private val DisconnectHitsThreshold = 5
	private val DefaultBanTimeout = TimeUnit.MINUTES.toMillis(1)
	private val ConnectionTimeout = TimeUnit.SECONDS.toMillis(30)

}