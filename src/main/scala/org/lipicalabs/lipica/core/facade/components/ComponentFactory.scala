package org.lipicalabs.lipica.core.facade.components

import java.net.URI

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db._
import org.lipicalabs.lipica.core.db.datasource.{KeyValueDataSource, LevelDbDataSource}
import org.lipicalabs.lipica.core.facade.listener.CompositeLipicaListener
import org.lipicalabs.lipica.core.kernel.Wallet
import org.lipicalabs.lipica.core.net.channel.ChannelManager
import org.lipicalabs.lipica.core.net.peer_discovery.active_discovery.PeerDiscovery
import org.lipicalabs.lipica.core.net.peer_discovery.udp.UDPListener
import org.lipicalabs.lipica.core.net.peer_discovery.{Node, NodeManager}
import org.lipicalabs.lipica.core.sync.{PeersPool, SyncManager, SyncQueue}
import org.lipicalabs.lipica.core.validator.block_header_rules.{BlockHeaderValidator, ExtraDataRule, ManaValueRule, ProofOfWorkRule}
import org.lipicalabs.lipica.core.validator.block_rules.{BlockValidator, TxTrieRootRule, UnclesRule}
import org.lipicalabs.lipica.core.validator.parent_rules.{DifficultyRule, ParentBlockHeaderValidator, ParentNumberRule}
import org.lipicalabs.lipica.core.vm.program.invoke.{ProgramInvokeFactory, ProgramInvokeFactoryImpl}


/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 15:08
 * YANAGISAWA, Kentaro
 */
object ComponentFactory {

	def createBlockStore: BlockStore = {
		val hashToBlockDB = createKeyValueDataSource("hash2block_db")
		hashToBlockDB.init()

		val numberToBlocksDB = createKeyValueDataSource("number2blocks_db")
		numberToBlocksDB.init()

		IndexedBlockStore.newInstance(hashToBlockDB, numberToBlocksDB)
	}

	def createRepository: Repository = {
		val detailsDS = createKeyValueDataSource("")
		val stateDS = createKeyValueDataSource("")
		new RepositoryImpl(detailsDS, stateDS)
	}

	def createWallet: Wallet = new Wallet

	def createAdminInfo: AdminInfo = new AdminInfo

	def createListener: CompositeLipicaListener = new CompositeLipicaListener

	def createKeyValueDataSource(name: String): KeyValueDataSource = new LevelDbDataSource(name)

	def createBlockValidator: BlockValidator = {
		val rules = Seq(new TxTrieRootRule, new UnclesRule)
		new BlockValidator(rules)
	}

	def createBlockHeaderValidator: BlockHeaderValidator = {
		val rules = Seq(new ManaValueRule, new ExtraDataRule, new ProofOfWorkRule)
		new BlockHeaderValidator(rules)
	}

	def createParentHeaderValidator: ParentBlockHeaderValidator = {
		val rules = Seq(new ParentNumberRule, new DifficultyRule)
		new ParentBlockHeaderValidator(rules)
	}

	def createChannelManager: ChannelManager = {
		val result = new ChannelManager
		result.init()
		result
	}

	def createPeerDiscovery: PeerDiscovery = new PeerDiscovery

	def createNodeManager: NodeManager = {
		val result = NodeManager.create
		result.seedNodes = SystemProperties.CONFIG.seedNodes.map(s => URI.create(s)).map(uri => Node(uri))
		result
	}

	def createSyncManager: SyncManager = new SyncManager

	def createSyncQueue: SyncQueue = {
		val hashStoreDB = createKeyValueDataSource("hashstore_db")
		hashStoreDB.init()

		new SyncQueue(hashStoreDB)
	}

	def createPeersPool: PeersPool = {
		val result = new PeersPool
		result.init()
		result
	}

	def createUDPListener: UDPListener = new UDPListener

	def createProgramInvokeFactory: ProgramInvokeFactory = new ProgramInvokeFactoryImpl

}
