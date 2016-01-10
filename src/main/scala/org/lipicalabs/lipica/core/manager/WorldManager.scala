package org.lipicalabs.lipica.core.manager

import java.io.Closeable
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.base._
import org.lipicalabs.lipica.core.config.{SystemProperties, ComponentFactory}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db._
import org.lipicalabs.lipica.core.listener.{CompositeLipicaListener, LipicaListener}
import org.lipicalabs.lipica.core.net.endpoint.PeerClient
import org.lipicalabs.lipica.core.net.lpc.sync.{PeersPool, SyncQueue, SyncManager}
import org.lipicalabs.lipica.core.net.channel.ChannelManager
import org.lipicalabs.lipica.core.net.peer_discovery.NodeManager
import org.lipicalabs.lipica.core.net.peer_discovery.active_discovery.PeerDiscovery
import org.lipicalabs.lipica.core.net.peer_discovery.udp.UDPListener
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.program.invoke.{ProgramInvokeFactoryImpl, ProgramInvokeFactory}
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 16:08
 * YANAGISAWA, Kentaro
 */
class WorldManager extends Closeable {

	import WorldManager._

	val blockStore: BlockStore = ComponentFactory.createBlockStore

	val repository: Repository = ComponentFactory.createRepository

	val wallet: Wallet = ComponentFactory.createWallet

	val adminInfo: AdminInfo = ComponentFactory.createAdminInfo

	val listener: CompositeLipicaListener = ComponentFactory.createListener

	val blockHeaderValidator = ComponentFactory.createBlockHeaderValidator

	val parentHeaderValidator = ComponentFactory.createParentHeaderValidator

	val blockchain: Blockchain = new BlockchainImpl(this.blockStore, this.repository, this.wallet, this.adminInfo, this.listener, this.parentHeaderValidator)

	val peerDiscovery: PeerDiscovery = ComponentFactory.createPeerDiscovery

	val channelManager: ChannelManager = ComponentFactory.createChannelManager

	val nodeManager: NodeManager = ComponentFactory.createNodeManager

	val syncManager: SyncManager = ComponentFactory.createSyncManager

	val syncQueue: SyncQueue = ComponentFactory.createSyncQueue

	val peersPool: PeersPool = ComponentFactory.createPeersPool

	val udpListener: UDPListener = ComponentFactory.createUDPListener

	val programInvokeFactory: ProgramInvokeFactory = ComponentFactory.createProgramInvokeFactory

	val blockLoader: BlockLoader = new BlockLoader

	def addListener(listener: LipicaListener): Unit = this.listener.addListener(listener)

	def startPeerDiscovery(): Unit = {
		if (!this.peerDiscovery.isStarted) {
			this.peerDiscovery.start()
		}
	}

	def stopPeerDiscovery(): Unit = {
		if (this.peerDiscovery.isStarted) {
			this.peerDiscovery.stop()
		}
	}

	private val clientRef = new AtomicReference[PeerClient](null)
	def client: PeerClient = this.clientRef.get


	def init(): Unit = {
		this.blockchain.asInstanceOf[BlockchainImpl].programInvokeFactory = this.programInvokeFactory
		this.programInvokeFactory.asInstanceOf[ProgramInvokeFactoryImpl].blockchain = this.blockchain

		val coinbaseAddress = DigestUtils.digest256(SystemProperties.CONFIG.coinbaseSecret.getBytes(StandardCharsets.UTF_8))
		this.wallet.importKey(ImmutableBytes(coinbaseAddress))

		//データベースからブロックを読み込む。
		loadBlockchain()
		//ファイルからブロックを読み込む。
		this.blockLoader.loadBlocks(this.blockchain)

		this.clientRef.set(new PeerClient)

		this.udpListener.start()
		this.syncManager.start()
	}

	def loadBlockchain(): Unit = {
		if (!SystemProperties.CONFIG.databaseReset) {
			this.blockStore.load()
		}

		this.blockStore.getBestBlock match {
			case Some(bestBlock) =>
				this.blockchain.bestBlock = bestBlock
				val totalDifficulty = this.blockStore.getTotalDifficulty
				this.blockchain.totalDifficulty = totalDifficulty
				logger.info("<WorldManager> Loaded up to block %,d ; TD=%,d ; StateRoot=%s".format(this.blockchain.bestBlock.blockNumber, this.blockchain.totalDifficulty, this.blockchain.bestBlock.stateRoot.toHexString))
			case None =>
				logger.info("<WorldManager> DB is empty. Adding the Genesis block.")
				val genesis = Genesis.getInstance
				for (entry <- genesis.premine) {
					this.repository.createAccount(entry._1)
					this.repository.addBalance(entry._1, entry._2.balance)
				}
				this.blockStore.saveBlock(genesis, genesis.cumulativeDifficulty, mainChain = true)
				this.blockchain.bestBlock = genesis
				this.blockchain.totalDifficulty = genesis.cumulativeDifficulty

				this.listener.onBlock(genesis, Seq.empty)
				this.repository.dumpState(genesis, 0, 0, null)
				logger.info("<WorldManager> Genesis block is loaded.")
		}

		if (this.blockchain.bestBlock.stateRoot != DigestUtils.EmptyTrieHash) {
			this.repository.syncToRoot(this.blockchain.bestBlock.stateRoot)
		}
	}

	def flush(): Unit = {
		this.repository.flush()
		this.blockStore.flush()
	}

	override def close(): Unit = {
		stopPeerDiscovery()
		flush()
		this.repository.close()
		this.blockchain.close()
	}
}

object WorldManager {
	private val logger = LoggerFactory.getLogger("general")

	val instance: WorldManager = createWorldManager

	private def createWorldManager: WorldManager = {
		val result = new WorldManager
		result.init()
		result
	}

}
