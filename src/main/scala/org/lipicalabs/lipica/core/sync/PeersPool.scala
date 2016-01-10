package org.lipicalabs.lipica.core.sync

import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.net.channel.Channel
import org.lipicalabs.lipica.core.net.peer_discovery.Node
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * 現在自ノードが通信しているピアを格納し管理するためのクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:24
 * YANAGISAWA, Kentaro
 */
class PeersPool {
	import PeersPool._
	import scala.collection.JavaConversions._

	private val activePeers: mutable.Map[ImmutableBytes, Channel] = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, Channel])
	private val bannedPeers: mutable.Map[Channel, BanReason] = mapAsScalaConcurrentMap(new ConcurrentHashMap[Channel, BanReason])
	private val disconnectHits: mutable.Map[ImmutableBytes, Int] = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, Int])
	private val bans: mutable.Map[ImmutableBytes, Long] = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, Long])
	private val pendingConnections: mutable.Map[ImmutableBytes, Long] = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, Long])

	private def lipica: Lipica = Lipica.instance

	/**
	 * 生成後に１回だけ実行される初期化処理です。
	 */
	def init(): Unit = {
		//定期処理を登録し実行します。
		Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("peers-pool")).scheduleWithFixedDelay(
			new Runnable {
				override def run(): Unit = {
					releaseBans()
					processConnections()
				}
			}, WorkerTimeoutSeconds, WorkerTimeoutSeconds, TimeUnit.SECONDS
		)
	}

	/**
	 * 渡されたノードをアクティブなものとして追加します。
	 * ChannelManager -> SyncManager という経路で呼びだされます。
	 */
	def add(peer: Channel): Unit = {
		this.activePeers.synchronized {
			this.activePeers.put(peer.nodeId, peer)
			this.bannedPeers.remove(peer)
		}
		this.pendingConnections.synchronized {
			this.pendingConnections.remove(peer.nodeId)
		}
		this.bans.synchronized {
			this.bans.remove(peer.nodeId)
		}
		logger.info("<PeersPool> %s. ADDED to pool.".format(peer.nodeIdShort))
	}

	/**
	 * active peerの中から、最大のTDを持つノードを選択して返します。
	 */
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

	/**
	 * active peerの中から、指定されたIDを持つノードを選択して返します。
	 */
	def getByNodeId(nodeId: ImmutableBytes): Option[Channel] = this.activePeers.get(nodeId)

	def onDisconnect(peer: Channel): Unit = {
		if ((peer eq null) || isNullOrEmpty(peer.nodeId)) {
			return
		}
		if (logger.isTraceEnabled) {
			logger.trace("<PeersPool> Peer %s: disconnected.".format(peer.nodeIdShort))
		}
		this.activePeers.synchronized {
			this.activePeers.remove(peer.nodeId).foreach {
				_ => logger.info("<PeersPool> %s. REMOVED from pool (disconnection).".format(peer.nodeIdShort))
			}
			this.bannedPeers.remove(peer)
		}

		this.disconnectHits.synchronized {
			//接続断が頻発するようであれば反省させる。
			val hits = this.disconnectHits.getOrElse(peer.nodeId, 0)
			if (DisconnectHitsThreshold < hits) {
				ban(peer, FrequentDisconnects)
				logger.info("<PeersPool> Banning a peer: Peer %s is banned due to frequent disconnections.".format(peer.nodeIdShort))
				this.disconnectHits.remove(peer.nodeId)
			} else {
				this.disconnectHits.put(peer.nodeId, hits + 1)
			}
		}
	}

	/**
	 * 指定されたノードに接続を試みます。
	 * SyncManager の fill up 処理によって呼び出されます。
	 */
	def connect(node: Node): Unit = {
		if (logger.isTraceEnabled) {
			logger.trace("<PeersPool> Peer %s: initiating connection.".format(node.id.toShortString))
		}

		if (isInUse(node.id)) {
			//既に認識済みのノードである。
			if (logger.isTraceEnabled) {
				logger.trace("<PeersPool> Peer %s: already initiated.".format(node.id.toShortString))
			}
			return
		}
		logger.info("<PeersPool> CONNECTING to %s (%s).".format(node.id.toShortString, node.address))
		this.pendingConnections.synchronized {
			lipica.connect(node)
			this.pendingConnections.put(node.id, System.currentTimeMillis + ConnectionTimeout)
		}
	}

	/**
	 * 指定されたノードを、banリストに登録します。
	 */
	def ban(peer: Channel, reason: BanReason): Unit = {
		peer.changeSyncState(SyncStateName.Idle)
		this.activePeers.synchronized {
			if (this.activePeers.contains(peer.nodeId)) {
				this.activePeers.remove(peer.nodeId)
				this.bannedPeers.put(peer, reason)
			}
		}
		this.bans.synchronized {
			this.bans.put(peer.nodeId, System.currentTimeMillis + DefaultBanTimeout)
		}
		logger.info("<PeersPool> Banned the peer: %s".format(peer.nodeIdShort))
	}

	/**
	 * このインスタンスにとって既知であるノードの集合を返します。
	 */
	def nodesInUse: Set[ImmutableBytes] = {
		var result: Set[ImmutableBytes] =
			this.activePeers.synchronized {
				this.activePeers.values.map(_.nodeId).toSet
			}
		result ++= this.bans.synchronized(this.bans.keySet)
		result ++= this.pendingConnections.synchronized(this.pendingConnections.keySet)

		result
	}

	def isInUse(nodeId: ImmutableBytes): Boolean = nodesInUse.contains(nodeId)

	/**
	 * すべてのピアについて、状態を指定されたものに遷移させようとします。
	 *
	 * @param stateName 遷移先の状態。
	 */
	def changeState(stateName: SyncStateName): Unit = {
		changeState(stateName, each => true)
	}

	/**
	 * 条件に合致したピアについて、状態を指定されたものに遷移させようとします。
	 *
	 * @param stateName 遷移先の状態。
	 * @param predicate 条件。
	 */
	def changeState(stateName: SyncStateName, predicate: (Channel) => Boolean): Unit = {
		this.activePeers.synchronized {
			this.activePeers.values.withFilter(each => predicate(each)).foreach {
				each => each.changeSyncState(stateName)
			}
		}
	}

	def isEmpty: Boolean = this.activePeers.isEmpty

	def activeCount: Int = this.activePeers.size

	def pendingCount: Int = this.pendingConnections.size

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

	def bannedPeersMap: Map[ImmutableBytes, BanReason] = {
		this.synchronized {
			this.bannedPeers.map(entry => entry._1.nodeId -> entry._2).toMap
		}
	}

	/**
	 * 時間が経過したban対象を赦免し、active peersに戻します。
	 */
	private def releaseBans(): Unit = {
		var released: Set[ImmutableBytes] = Set.empty
		this.bans.synchronized {
			released = getTimeoutExceeded(this.bans)
			this.activePeers.synchronized {
				for (peer <- this.bannedPeers.keys) {
					if (released.contains(peer.nodeId)) {
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

	/**
	 * pending状態のノードのうち、一定の時間が経過したものを忘れます。
	 */
	private def processConnections(): Unit = {
		this.pendingConnections.synchronized {
			getTimeoutExceeded(this.pendingConnections).foreach {
				each => this.pendingConnections.remove(each)
			}
		}
	}
}

object PeersPool {

	private val logger = LoggerFactory.getLogger("sync")
	private val WorkerTimeoutSeconds = 3L
	private val DisconnectHitsThreshold = 5
	private val DefaultBanTimeout = TimeUnit.MINUTES.toMillis(1)
	private val ConnectionTimeout = TimeUnit.SECONDS.toMillis(30)

	private def isNullOrEmpty(nodeId: ImmutableBytes): Boolean = {
		(nodeId eq null) || nodeId.isEmpty
	}

	private def getTimeoutExceeded(map: mutable.Map[ImmutableBytes, Long]): Set[ImmutableBytes] = {
		//渡された連想配列の要素を時刻として解釈し、すでにその時刻が到来している要素の集合を返す。
		val now = System.currentTimeMillis
		map.withFilter(entry => entry._2 <= now).map(entry => entry._1).toSet
	}

}

sealed trait BanReason

case object FrequentDisconnects extends BanReason
case object BlocksLack extends BanReason
case object InvalidBlock extends BanReason
case object Stuck extends BanReason