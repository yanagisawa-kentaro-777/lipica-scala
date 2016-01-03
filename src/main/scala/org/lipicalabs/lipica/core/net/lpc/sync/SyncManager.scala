package org.lipicalabs.lipica.core.net.lpc.sync

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import org.lipicalabs.lipica.core.base.{Blockchain, BlockWrapper}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.channel.{ChannelManager, Channel}
import org.lipicalabs.lipica.core.net.transport.discover.{NodeStatistics, DiscoverListener, NodeHandler, NodeManager}
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:51
 * YANAGISAWA, Kentaro
 */
class SyncManager {
	import SyncManager._

	private val syncStates: Map[SyncStateName, SyncState] = buildSyncStates(this)
	private var state: SyncState = null
	private val stateMutex = new Object

	private var gapBlock: BlockWrapper = null
	def getGapBlock: BlockWrapper = this.gapBlock
	def resetGapRecovery(): Unit = this.gapBlock = null

	private var syncDone: Boolean = false
	def isSyncDone: Boolean = this.syncDone

	private var lowerUsefulDiffuculty = UtilConsts.Zero
	private var highestKnownDiffuculty = UtilConsts.Zero

	private val worker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor


	private def worldManager: WorldManager = WorldManager.instance
	private def blockchain: Blockchain = worldManager.blockchain
	private def nodeManager: NodeManager = worldManager.nodeManager
	private def lipicaListener: LipicaListener = worldManager.listener
	private def channelManager: ChannelManager = worldManager.channelManager

	def queue: SyncQueue = worldManager.syncQueue
	def pool: PeersPool = worldManager.peersPool


	def start(): Unit = {
		val task = new Runnable {
			override def run(): Unit = {
				queue.init()
				if (!SystemProperties.CONFIG.isSyncEnabled) {
					logger.info("<SyncManager> SyncManager: OFF.")
					return
				}
				logger.info("<SyncManager> SyncManager: ON.")
				state = syncStates.get(SyncStateName.Idle).get

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
									logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
							}
						}
					}, WorkerTimeout, WorkerTimeout, TimeUnit.MILLISECONDS
				)
				SystemProperties.CONFIG.activePeers.foreach(each => pool.connect(each))
				if (logger.isInfoEnabled) {
					startLogWorker()
				}
			}
		}
		new Thread(task).start()
	}

	def addPeer(peer: Channel): Unit = {
		if (!SystemProperties.CONFIG.isSyncEnabled) {
			return
		}
		if (logger.isTraceEnabled) {
			logger.trace("<SyncManager> Adding a peer: %s".format(peer.peerIdShort))
		}
		val peerTotalDifficulty = peer.totalDifficulty
		if (!isIn20PercentRange(peerTotalDifficulty, this.lowerUsefulDiffuculty)) {
			if (logger.isTraceEnabled) {
				logger.trace("<SyncManager> Peer %s: Difficulty is significantly lower than ours (%,d < %,d). Skipping.".format(peer.peerIdShort, peerTotalDifficulty, this.lowerUsefulDiffuculty))
			}
			return
		}
		if (this.state.is(SyncStateName.HashRetrieving) && !isIn20PercentRange(highestKnownDiffuculty, peerTotalDifficulty)) {
			if (logger.isTraceEnabled) {
				logger.trace("<SyncManager> Peer %s: Chain is better than the known best: (%,d < %,d). Rotating master peer.".format(peer.peerIdShort, this.highestKnownDiffuculty, peerTotalDifficulty))
			}
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
			return
		}
		if (logger.isDebugEnabled) {
			logger.debug("<SyncManager> Recovering gap: best.number %,d vs block.number %s".format(this.blockchain.bestBlock.blockNumber, blockWrapper.blockNumber))
		}
		this.gapBlock = blockWrapper
		val gap = gapSize(blockWrapper)
		if (LargeGapSize <= gap) {
			changeState(SyncStateName.HashRetrieving)
		} else {
			logger.info("<SyncManager> Forcing parent downloading for %,d".format(blockWrapper.blockNumber))
			this.queue.addHash(blockWrapper.parentHash)
		}
	}

	def notifyNewBlockImported(wrapper: BlockWrapper): Unit = {
		if (this.syncDone) {
			return
		}
		if (!wrapper.isSolidBlock) {
			//最新情報をインポートできるとは、完全に追いついたようだ。
			this.syncDone = true
			onSyncDone()
			if (logger.isDebugEnabled) {
				logger.debug("<SyncManager> New block %,d is imported. Sync is complete.".format(wrapper.blockNumber))
			}
		} else if (logger.isDebugEnabled) {
			logger.debug("<SyncManager> New block %,d is imported. Continuing to sync.".format(wrapper.blockNumber))
		}
	}

	def reportInvalidBlock(nodeId: ImmutableBytes): Unit = {
		this.pool.getByNodeId(nodeId).foreach {
			peer => {
				logger.info("<SyncManager> Banning a peer: Peer %s: Invalid block received.".format(peer.peerIdShort))
				this.pool.ban(peer)
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
			//まだそんな段階ではない。
			return false
		}
		if ((block == this.gapBlock) && !this.state.is(SyncStateName.Idle)) {
			if (logger.isTraceEnabled) {
				logger.trace("<SyncManager> Gap recovery is already in progress for %,d".format(this.gapBlock.blockNumber))
			}
			return false
		}
		if (this.queue.isHashesEmpty) {
			//ブロックのダウンロードは完了しているのに、ギャップがあるということだ。やろう。
			return true
		}
		//ブロックをダウンロード中である。新ブロックならidleになるまで待つべきである。
		if (!block.isNewBlock) {
			GapRecoveryTimeout < block.timeSinceFailed
		} else {
			this.state.is(SyncStateName.Idle)
		}
	}

	def changeState(stateName: SyncStateName): Unit = {
		this.syncStates.get(stateName).foreach {
			newState => {
				if (newState == this.state) {
					return
				}
				logger.info("<SyncManager> Changin state from %s to %s".format(this.state, newState))
				this.stateMutex.synchronized {
					newState.doOnTransition()
					this.state = newState
				}
			}
		}
	}

	def isPeerStuck(peer: Channel): Boolean = {
		val stats = peer.getSyncStats
		(PeerStuckTimeout < stats.millisSinceLastUpdate) || (0 < stats.emptyResponsesCount)
	}

	def startMaster(master: Channel): Unit = {
		this.pool.changeState(SyncStateName.Idle)
		if (this.gapBlock ne null) {
			master.lastHashToAsk = this.gapBlock.parentHash
		} else {
			master.lastHashToAsk = master.bestKnownHash
			this.queue.clearHashes()
		}
		master.changeSyncState(SyncStateName.HashRetrieving)
		logger.info("<SyncManager> Peer %s: %s initiated. LastHashToAsk=%s, AskLimit=%,d".format(master.peerIdShort, this.state, master.lastHashToAsk, master.maxHashesAsk))
	}


	private def updateDifficulties(): Unit = {
		updateLowerUsefulDifficulty(this.blockchain.totalDifficulty)
		updateHighestKnownDifficulty(this.blockchain.totalDifficulty)
	}

	private def updateLowerUsefulDifficulty(difficulty: BigInt): Unit = {
		if (this.lowerUsefulDiffuculty < difficulty) {
			this.lowerUsefulDiffuculty = difficulty
		}
	}

	private def updateHighestKnownDifficulty(difficulty: BigInt): Unit = {
		if (this.highestKnownDiffuculty < difficulty) {
			this.highestKnownDiffuculty = difficulty
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
					logger.trace("<SyncManager> Peer %s: new best chain peer discovered: %,d vs %,d".format(handler.node.hexIdShort, handler.nodeStatistics.lpcTotalDifficulty, highestKnownDiffuculty))
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
				!isIn20PercentRange(this.highestKnownDiffuculty, nodeStats.lpcTotalDifficulty)
			}
		)

	}

	private def startLogWorker(): Unit = {
		Executors.newSingleThreadScheduledExecutor.scheduleWithFixedDelay(
			new Runnable {
				override def run(): Unit = {
					try {
						pool.logActivePeers()
						pool.logBannedPeers()
						logger.info("State: %s".format(state))
					} catch {
						case e: Throwable =>
							logger.warn("<SyncManager> Exception caught: %s".format(e.getClass.getSimpleName), e)
					}
				}
			}, 0, 30, TimeUnit.SECONDS
		)
	}

	private def removeUselessPeers(): Unit = {
		val removed = this.pool.peers.filter(_.hasBlocksLack)
		for (each <- removed) {
			logger.info("<SyncManager> Banning a peer: Peer %s has no more blocks. Removing.".format(each.peerIdShort))
			this.pool.ban(each)
			updateLowerUsefulDifficulty(each.totalDifficulty)
		}
	}

	private def fillUpPeersPool(): Unit = {
		val lackSize = SystemProperties.CONFIG.syncPeersCount - this.pool.activeCount
		if (lackSize <= 0) {
			return
		}
		val nodesInUse = this.pool.nodesInUse
		var newNodes = this.nodeManager.getBestLpcNodes(nodesInUse, this.lowerUsefulDiffuculty, lackSize)
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
					nodes.map(_.node.hexIdShort).mkString(",")
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
		result.values.foreach(each => each.asInstanceOf[AbstractSyncState].syncManager = syncManager)
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