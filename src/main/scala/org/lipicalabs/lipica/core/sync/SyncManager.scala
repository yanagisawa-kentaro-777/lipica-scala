package org.lipicalabs.lipica.core.sync

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{ScheduledExecutorService, TimeUnit}

import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.kernel.{Blockchain, BlockWrapper}
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.listener.LipicaListener
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.channel.{ChannelManager, Channel}
import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeStatistics, NodeManager, NodeHandler}
import org.lipicalabs.lipica.core.net.peer_discovery.discover.DiscoverListener
import org.lipicalabs.lipica.core.utils.{ErrorLogger, UtilConsts}
import org.slf4j.LoggerFactory

/**
 * 先行する他ノードの情報を自ノードに同期する際の
 * 中核もしくはハブとなるクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:51
 * YANAGISAWA, Kentaro
 */
class SyncManager {
	import SyncManager._

	/**
	 * 自ノードの基本的な同期状態。
	 */
	private val syncStates: Map[SyncStateName, SyncState] = buildSyncStates(this)
	private val stateRef: AtomicReference[SyncState] = new AtomicReference[SyncState](null)
	def state: SyncState = this.stateRef.get
	/**
	 * 排他制御オブジェクト。
	 */
	private val stateMutex = new Object

	/**
	 * 早期解決を要する未取得ブロック。
	 */
	private val gapBlockRef: AtomicReference[BlockWrapper] = new AtomicReference[BlockWrapper](null)
	def gapBlockOption: Option[BlockWrapper] = Option(this.gapBlockRef.get)
	def resetGapRecovery(): Unit = this.gapBlockRef.set(null)

	private val syncDoneRef: AtomicBoolean = new AtomicBoolean(false)
	def isSyncDone: Boolean = this.syncDoneRef.get

	private val lowerUsefulDifficultyRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def lowerUsefulDifficulty: BigInt = this.lowerUsefulDifficultyRef.get

	private val highestKnownDifficultyRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def highestKnownDifficulty: BigInt = this.highestKnownDifficultyRef.get

	private val worker: ScheduledExecutorService = ExecutorPool.instance.syncManagerProcessor


	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance
	private def blockchain: Blockchain = componentsMotherboard.blockchain
	private def nodeManager: NodeManager = componentsMotherboard.nodeManager
	private def lipicaListener: LipicaListener = componentsMotherboard.listener
	private def channelManager: ChannelManager = componentsMotherboard.channelManager

	def queue: SyncQueue = componentsMotherboard.syncQueue
	def pool: PeersPool = componentsMotherboard.peersPool


	def start(): Unit = {
		val task = new Runnable {
			override def run(): Unit = {
				queue.init()
				if (!NodeProperties.instance.isSyncEnabled) {
					logger.info("<SyncManager> SyncManager: OFF.")
					return
				}
				logger.info("<SyncManager> SyncManager: ON.")
				stateRef.set(syncStates.get(SyncStateName.Idle).get)

				updateDifficulties()
				changeState(initialState())
				addBestKnownNodeListener()
				worker.scheduleWithFixedDelay(
					new Runnable {
						override def run(): Unit = {
							try {
								updateDifficulties()
								removeUselessPeers()
								fillUpPeersPool()
								maintainState()
							} catch {
								case e: Throwable =>
									ErrorLogger.logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
									logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
							}
						}
					}, WorkerTimeout, WorkerTimeout, TimeUnit.MILLISECONDS
				)
				//NodeProperties.CONFIG.activePeers.foreach(each => pool.connect(each))
				if (logger.isInfoEnabled) {
					startLogWorker()
				}
			}
		}
		ExecutorPool.instance.syncManagerStarter.execute(task)
	}

	def addPeer(peer: Channel): Unit = {
		if (!NodeProperties.instance.isSyncEnabled) {
			return
		}
		if (logger.isTraceEnabled) {
			logger.trace("<SyncManager> Adding a peer: %s".format(peer.nodeIdShort))
		}
		val peerTotalDifficulty = peer.totalDifficulty
		if (!isIn20PercentRange(peerTotalDifficulty, this.lowerUsefulDifficulty)) {
			if (logger.isDebugEnabled) {
				logger.debug("<SyncManager> Peer %s: Difficulty is significantly lower than ours (%,d < %,d). Skipping.".format(peer.nodeIdShort, peerTotalDifficulty, this.lowerUsefulDifficulty))
			}
			return
		}
		if (this.state.is(SyncStateName.HashRetrieving) && !isIn20PercentRange(highestKnownDifficulty, peerTotalDifficulty)) {
			logger.info("<SyncManager> Peer %s: Chain is better than the known best: (%,d < %,d). Rotating master peer.".format(peer.nodeIdShort, this.highestKnownDifficulty, peerTotalDifficulty))
			this.stateMutex.synchronized {
				startMaster(peer)
			}
		}
		updateHighestKnownDifficulty(peerTotalDifficulty)
		this.pool.add(peer)
	}

	def onDisconnect(peer: Channel): Unit = {
		if (peer.isHashRetrieving || peer.isHashRetrievingDone) {
			changeState(SyncStateName.BlockRetrieving)
		}
		this.pool.onDisconnect(peer)
	}

	def tryGapRecovery(blockWrapper: BlockWrapper): Unit = {
		if (!isGapRecoveryAllowed(blockWrapper)) {
			logger.debug("<SyncManager> Don't start gap recovery.")
			return
		}
		if (logger.isDebugEnabled) {
			logger.debug("<SyncManager> Recovering gap: Best=%,d vs Pending=%s".format(this.blockchain.bestBlock.blockNumber, blockWrapper.blockNumber))
		}
		this.gapBlockRef.set(blockWrapper)
		val gap = gapSize(blockWrapper)
		if (LargeGapSize <= gap) {
			changeState(SyncStateName.HashRetrieving)
		} else {
			logger.info("<SyncManager> Forcing parent downloading for %,d".format(blockWrapper.blockNumber))
			this.queue.addHash(blockWrapper.parentHash)
		}
	}

	def notifyNewBlockImported(wrapper: BlockWrapper): Unit = {
		if (this.isSyncDone) {
			return
		}
		if (!wrapper.isSolidBlock) {
			//最新情報をインポートできるとは、完全に追いついたようだ。
			this.syncDoneRef.set(true)
			onSyncDone()
			if (logger.isDebugEnabled) {
				logger.debug("<SyncManager> New block %,d is imported. Sync is complete.".format(wrapper.blockNumber))
			}
		} else if (logger.isDebugEnabled) {
			logger.debug("<SyncManager> New block %,d is imported. Continuing to sync.".format(wrapper.blockNumber))
		}
	}

	def reportInvalidBlock(nodeId: NodeId): Unit = {
		this.pool.getByNodeId(nodeId).foreach {
			peer => {
				logger.info("<SyncManager> Banning a peer: Peer %s: Invalid block received.".format(peer.nodeIdShort))
				this.pool.ban(peer, InvalidBlock)
			}
		}
	}

	private def gapSize(blockWrapper: BlockWrapper): Int = {
		(blockWrapper.blockNumber - this.blockchain.bestBlock.blockNumber).toInt
	}

	private def onSyncDone(): Unit = {
		this.channelManager.onSyncDone()
		this.lipicaListener.onSyncDone()
		logger.info("<SyncManager> Main synchronization is done.")
	}

	private def isGapRecoveryAllowed(block: BlockWrapper): Boolean = {
		if (this.state.is(SyncStateName.HashRetrieving)) {
			//まだハッシュ値をダウンロードしている最中なのだから、ブロックのギャップを気にすべき段階ではない。
			if (logger.isDebugEnabled) {
				logger.debug("<SyncManager> Still retrieving hashes. No need to recover gap.")
			}
			return false
		}
		if (this.gapBlockOption.contains(block) && !this.state.is(SyncStateName.Idle)) {
			//すでに同じ問題を解消するための取り組みを実施中である。
			if (logger.isTraceEnabled) {
				logger.trace("<SyncManager> Gap recovery is already in progress for %,d".format(this.gapBlockOption.map(_.blockNumber).getOrElse(-1L)))
			}
			return false
		}
		if (this.queue.isHashesEmpty) {
			//ブロックのダウンロードは完了しているのに、ギャップがあるということだ。やろう。
			return true
		}
		//ブロックをダウンロード中である。新ブロックならidleになるまで待つべきである。
		if (!block.isNewBlock) {
			val elapsed = block.timeSinceFailed
			val ok = GapRecoveryTimeout < elapsed
			if (!ok) {
				logger.debug("<SyncManager> Wait a while before starting gap recovery. (%,d < %,d)".format(elapsed, GapRecoveryTimeout))
			}
			ok
		} else {
			this.state.is(SyncStateName.Idle)
		}
	}

	/**
	 * 同期処理の状態をさせます。
	 */
	def changeState(stateName: SyncStateName): Unit = {
		this.syncStates.get(stateName).foreach {
			newState => {
				if (newState == this.state) {
					return
				}
				logger.info("<SyncManager> CHANGING STATE: %s -> %s".format(this.state, newState))
				this.stateMutex.synchronized {
					newState.doOnTransition()
					this.stateRef.set(newState)
				}
			}
		}
	}

	def isPeerStuck(peer: Channel): Boolean = {
		val stats = peer.getSyncStats
		(PeerStuckTimeout < stats.millisSinceLastUpdate) || (0 < stats.emptyResponsesCount)
	}

	/**
	 * 指定されたピアを新たなマスターとして、ダイジェスト値の収集を開始します。
	 */
	def startMaster(master: Channel): Unit = {
		this.pool.changeState(SyncStateName.Idle)
		this.gapBlockOption match {
			case Some(gapBlock) =>
				//ギャップの解決中ならば、その直前の取得を優先する。
				master.lastHashToAsk = gapBlock.parentHash
			case _ =>
				//ギャップ解決中でないならば、最先端から逆順に取得する。
				master.lastHashToAsk = master.bestKnownHash
				this.queue.clearHashes()
		}
		master.changeSyncState(SyncStateName.HashRetrieving)
		logger.info("<SyncManager> Peer %s: %s initiated. LastHashToAsk=%s, AskLimit=%,d".format(master.nodeIdShort, this.state, master.lastHashToAsk, master.maxHashesAsk))
	}


	private def updateDifficulties(): Unit = {
		updateLowerUsefulDifficulty(this.blockchain.totalDifficulty)
		updateHighestKnownDifficulty(this.blockchain.totalDifficulty)
	}

	private def updateLowerUsefulDifficulty(difficulty: BigInt): Unit = {
		if ((this.lowerUsefulDifficulty < difficulty) && (difficulty <= this.highestKnownDifficulty)) {
			this.lowerUsefulDifficultyRef.set(difficulty)
		}
	}

	private def updateHighestKnownDifficulty(difficulty: BigInt): Unit = {
		if (this.highestKnownDifficulty < difficulty) {
			this.highestKnownDifficultyRef.set(difficulty)
		}
	}

	private def initialState(): SyncStateName = {
		if (this.queue.hasSolidBlocks) {
			logger.info("<SyncManager> It seems that BlockRetrieving was interrupted. Resuming.")
			SyncStateName.BlockRetrieving
		} else {
			SyncStateName.HashRetrieving
		}
	}

	private def addBestKnownNodeListener(): Unit = {
		val listener = new DiscoverListener {
			override def nodeAppeared(handler: NodeHandler) = {
				if (logger.isTraceEnabled) {
					logger.trace("<SyncManager> Peer %s: new best chain peer discovered: %,d vs %,d".format(handler.node.id.toShortString, handler.nodeStatistics.lpcTotalDifficulty, highestKnownDifficulty))
				}
				pool.connect(handler.node)
			}
			override def nodeDisappeared(handler: NodeHandler) = {
				//
			}
		}
		this.nodeManager.addDiscoveryListener(
			listener,
			(nodeStats: NodeStatistics) => {
				!isIn20PercentRange(this.highestKnownDifficulty, nodeStats.lpcTotalDifficulty)
			}
		)

	}

	private def startLogWorker(): Unit = {
		ExecutorPool.instance.syncLogger.scheduleWithFixedDelay(
			new Runnable {
				override def run(): Unit = {
					try {
						pool.logActivePeers()
						pool.logBannedPeers()
						logger.info("State: %s".format(state))
					} catch {
						case e: Throwable =>
							ErrorLogger.logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
							logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
					}
				}
			}, 0, 30, TimeUnit.SECONDS
		)
	}

	private def removeUselessPeers(): Unit = {
		val removed = this.pool.peers.filter(_.hasBlocksLack)
		for (each <- removed) {
			logger.info("<SyncManager> Banning a peer: Peer %s has no more blocks. Removing.".format(each.nodeIdShort))
			this.pool.ban(each, BlocksLack)
			updateLowerUsefulDifficulty(each.totalDifficulty)
		}
	}

	private def fillUpPeersPool(): Unit = {
		val lackSize = NodeProperties.instance.syncPeersCount - this.pool.activeCount
		if (lackSize <= 0) {
			return
		}
		//十分なTDを持ち、なおかつ既知でないノードを選択する。
		val nodesInUse = this.pool.nodesInUse
		var newNodes = this.nodeManager.getBestLpcNodes(nodesInUse, this.lowerUsefulDifficulty, lackSize)
		if (this.pool.isEmpty && newNodes.isEmpty) {
			newNodes = this.nodeManager.getBestLpcNodes(nodesInUse, UtilConsts.Zero, lackSize)
		}
		if (logger.isTraceEnabled) {
			logDiscoveredNodes(newNodes)
		}
		if (logger.isDebugEnabled) {
			logger.debug("<SyncManager> Filling up nodes: Lacked=%,d ; Found=%,d".format(lackSize, newNodes.size))
		}
		newNodes.foreach(each => this.pool.connect(each.node))
	}

	private def logDiscoveredNodes(nodes: Seq[NodeHandler]): Unit = {
		if (logger.isTraceEnabled) {
			val found =
				if (nodes.nonEmpty) {
					nodes.map(_.node.id.toShortString).mkString(",")
				} else {
					"empty"
				}
			logger.trace("<SyncManager> Nodes obtained from discovery: %s".format(found))
		}
	}

	private def maintainState(): Unit = {
		this.stateMutex.synchronized {
			this.state.doMaintain()
		}
	}

}

object SyncManager {
	private val logger = LoggerFactory.getLogger("sync")

	private val WorkerTimeout = TimeUnit.SECONDS.toMillis(1)
	private val PeerStuckTimeout = TimeUnit.SECONDS.toMillis(60)
	private val GapRecoveryTimeout = TimeUnit.SECONDS.toMillis(2)
	private val LargeGapSize = 3L

	private def buildSyncStates(syncManager: SyncManager): Map[SyncStateName, SyncState] = {
		val result: Map[SyncStateName, SyncState] = Map(
			SyncStateName.Idle -> new IdleState,
			SyncStateName.HashRetrieving -> new HashRetrievingState,
			SyncStateName.BlockRetrieving -> new BlockRetrievingState
		)
		result.values.foreach(_.assign(syncManager))
		result
	}

	/**
	 * １番目の値が２番目✕80%以上であれば真を返します。
	 */
	private def isIn20PercentRange(first: BigInt, second: BigInt): Boolean = {
		if (second < first) {
			return true
		}
		val gap = second - first
		(gap * 5) <= first
	}
}