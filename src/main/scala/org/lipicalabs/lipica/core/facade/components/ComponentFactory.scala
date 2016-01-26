package org.lipicalabs.lipica.core.facade.components

import java.io.Closeable
import java.net.URI
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

import com.sleepycat.je.Environment
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.datastore._
import org.lipicalabs.lipica.core.datastore.datasource._
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

import scala.collection.JavaConversions

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

	class BdbDataSourceFactory(override val categoryName: String, env: Environment) extends KeyValueDataSourceFactory {
		private def dataSourceName(givenName: String) = "%s/%s".format(this.categoryName, givenName)
		override def openDataSource(name: String) = DataSourcePool.bdbByName(dataSourceName(name), env)
		override def closeDataSource(name: String) = DataSourcePool.closeDataSource(dataSourceName(name))
	}

	private val bdbEnv = BdbJeDataSource.createDefaultEnvironment(Paths.get(NodeProperties.CONFIG.databaseDir))
	def dataStoreResource: Closeable = this.bdbEnv

	val dataSources = JavaConversions.mapAsScalaConcurrentMap(new ConcurrentHashMap[String, KeyValueDataSource])
	private def put(dataSource: KeyValueDataSource): Unit = {
		this.dataSources.put(dataSource.name, dataSource)
	}

	def createBlockStore: BlockStore = {
		val hashToBlockDB = openKeyValueDataSource("hash2block_db")
		put(hashToBlockDB)
		val numberToBlocksDB = openKeyValueDataSource("number2blocks_db")
		put(numberToBlocksDB)
		IndexedBlockStore.newInstance(hashToBlockDB, numberToBlocksDB)
	}

	def createRepository: Repository = {
		val contractDS = openKeyValueDataSource("contract_dtl_db")
		put(contractDS)
		val stateDS = openKeyValueDataSource("state_db")
		put(stateDS)

		if (true) {
			//TODO leveldb or berkeley db.
			new RepositoryImpl(contractDS, stateDS, new BdbDataSourceFactory("contract_dtl_storage", this.bdbEnv))
		} else {
			new RepositoryImpl(contractDS, stateDS, new LevelDBDataSourceFactory("contract_dtl_storage"))
		}
	}

	def createWallet: Wallet = new Wallet

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
		put(dataSource)
		val result = NodeManager.create(dataSource)
		result.seedNodes = NodeProperties.CONFIG.seedNodes.map(s => URI.create(s)).map(uri => Node(uri))
		result
	}

	def createSyncManager: SyncManager = new SyncManager

	def createSyncQueue: SyncQueue = {
		val hashStoreDB = openKeyValueDataSource("hashstore_db")
		put(hashStoreDB)
		val queuedBlocksDB = openKeyValueDataSource("queued_blocks_db")
		put(queuedBlocksDB)
		val queuedHashesDB = openKeyValueDataSource("queued_hashes_db")
		put(queuedHashesDB)
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
		val result =
			if (true) {
				//TODO leveldb or berkeley db.
				val configs = BdbJeDataSource.createDefaultConfig
				new BdbJeDataSource(name, this.bdbEnv, configs)
			} else {
				val options = LevelDbDataSource.createDefaultOptions
				new LevelDbDataSource(name, options)
			}
		result.init()
		result
	}
}
