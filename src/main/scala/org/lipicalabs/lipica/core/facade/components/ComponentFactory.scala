package org.lipicalabs.lipica.core.facade.components

import java.net.URI

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db._
import org.lipicalabs.lipica.core.db.datasource.{DataSourcePool, KeyValueDataSourceFactory, KeyValueDataSource, LevelDbDataSource}
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
import org.lipicalabs.lipica.core.vm.program.context.{ProgramContextFactory, ProgramContextFactoryImpl}


/**
 * ノードの動作において重要なコンポーネントであるクラスのインスタンスを、
 * 生成し初期化して返すためのオブジェクトです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/25 15:08
 * YANAGISAWA, Kentaro
 */
object ComponentFactory {

	class LevelDBDataSourceFactory(override val categoryName: String) extends KeyValueDataSourceFactory {
		private def dataSourceName(givenName: String) = "%s/%s".format(this.categoryName, givenName)

		override def openDataSource(name: String) = DataSourcePool.levelDbByName(dataSourceName(name))

		override def closeDataSource(name: String) = DataSourcePool.closeDataSource(dataSourceName(name))
	}

	def createBlockStore: BlockStore = {
		val hashToBlockDB = openKeyValueDataSource("hash2block_db")
		val numberToBlocksDB = openKeyValueDataSource("number2blocks_db")
		IndexedBlockStore.newInstance(hashToBlockDB, numberToBlocksDB)
	}

	def createRepository: Repository = {
		val contractDS = openKeyValueDataSource("contract_dtl_db")
		val stateDS = openKeyValueDataSource("state_db")
		new RepositoryImpl(contractDS, stateDS, new LevelDBDataSourceFactory("contract_dtl_storage"))
	}

	def createWallet: Wallet = new Wallet

	def createAdminInfo: AdminInfo = new AdminInfo

	def createListener: CompositeLipicaListener = new CompositeLipicaListener

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
		val dataSource = openKeyValueDataSource("nodestats_db")
		val result = NodeManager.create(dataSource)
		result.seedNodes = SystemProperties.CONFIG.seedNodes.map(s => URI.create(s)).map(uri => Node(uri))
		result
	}

	def createSyncManager: SyncManager = new SyncManager

	def createSyncQueue: SyncQueue = {
		val hashStoreDB = openKeyValueDataSource("hashstore_db")
		val queuedBlocksDB = openKeyValueDataSource("queued_blocks_db")
		val queuedHashesDB = openKeyValueDataSource("queued_hashes_db")
		new SyncQueue(hashStoreDataSource = hashStoreDB, queuedBlocksDataSource = queuedBlocksDB, queuedHashesDataSource = queuedHashesDB)
	}

	def createPeersPool: PeersPool = {
		val result = new PeersPool
		result.init()
		result
	}

	def createUDPListener: UDPListener = new UDPListener

	def createProgramContextFactory: ProgramContextFactory = new ProgramContextFactoryImpl

	private def openKeyValueDataSource(name: String): KeyValueDataSource = {
		val options = LevelDbDataSource.createDefaultOptions
		val result = new LevelDbDataSource(name, options)
		result.init()
		result
	}
}
