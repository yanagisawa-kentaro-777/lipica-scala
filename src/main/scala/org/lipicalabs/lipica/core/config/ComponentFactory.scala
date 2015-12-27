package org.lipicalabs.lipica.core.config

import org.lipicalabs.lipica.core.base.Wallet
import org.lipicalabs.lipica.core.db.datasource.{KeyValueDataSource, HashMapDB, LevelDbDataSource}
import org.lipicalabs.lipica.core.db._
import org.lipicalabs.lipica.core.listener.CompositeLipicaListener
import org.lipicalabs.lipica.core.manager.AdminInfo
import org.lipicalabs.lipica.core.net.lpc.sync.{PeersPool, SyncQueue, SyncManager}
import org.lipicalabs.lipica.core.net.peer_discovery.PeerDiscovery
import org.lipicalabs.lipica.core.net.channel.ChannelManager
import org.lipicalabs.lipica.core.net.transport.discover.NodeManager
import org.lipicalabs.lipica.core.validator._
import org.mapdb.{Serializer, DBMaker}

import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 15:08
 * YANAGISAWA, Kentaro
 */
object ComponentFactory {

	def createBlockStore: BlockStore = {
		val databaseDir = SystemProperties.CONFIG.databaseDir
		val blocksIndexFile = new java.io.File(databaseDir + "/blocks/index")
		if (!blocksIndexFile.getParentFile.exists) {
			blocksIndexFile.getParentFile.mkdirs()
		}
		val indexDB = DBMaker.fileDB(blocksIndexFile).closeOnJvmShutdown().make()
		val coreMap = indexDB.hashMapCreate("index").keySerializer(Serializer.LONG).valueSerializer(BlockInfoSerializer).counterEnable.makeOrGet
		val indexMap: mutable.Map[Long, Seq[BlockInfo]] = JavaConversions.mapAsScalaMap(coreMap.asInstanceOf[java.util.Map[Long, Seq[BlockInfo]]])
		val blocksDB = new LevelDbDataSource("blocks")
		blocksDB.init()

		val cache = new IndexedBlockStore(new mutable.HashMap[Long, Seq[BlockInfo]], new HashMapDB, null, null)
		new IndexedBlockStore(indexMap, blocksDB, cache, indexDB)
	}

	def createRepository: Repository = {
		val detailsDS = new LevelDbDataSource("contract_details")
		val stateDS = new LevelDbDataSource("state")
		new RepositoryImpl(detailsDS, stateDS)
	}

	def createWallet: Wallet = new Wallet

	def createAdminInfo: AdminInfo = new AdminInfo

	def createListener: CompositeLipicaListener = new CompositeLipicaListener

	def createKeyValueDataSource(name: String): KeyValueDataSource = new LevelDbDataSource(name)

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

	def createNodeManager: NodeManager = NodeManager.create

	def createSyncManager: SyncManager = new SyncManager

	def createSyncQueue: SyncQueue = new SyncQueue

	def createPeersPool: PeersPool = {
		val result = new PeersPool
		result.init()
		result
	}

}
