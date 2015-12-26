package org.lipicalabs.lipica.core.config

import java.nio.file.{Paths, Path}
import java.util.concurrent.atomic.AtomicReference

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class SystemProperties(val config: Config) {

	def isStorageDictionaryEnabled: Boolean = {
		//TODO
		false
	}

	def isFrontier: Boolean = {
		//TODO
		true
	}

	def projectVersion: String = {
		//TODO
		"0.5.0.0"
	}

	def activePeers: Seq[Node] = {
		//TODO
		Seq.empty
	}


	def vmTrace: Boolean = this.config.getBoolean("vm.structured.trace")
	def vmTraceInitStorageLimit: Int = this.config.getInt("vm.structured.init.storage.limit")
	def dumpBlock: Long = this.config.getLong("dump.block")
	def dumpDir: String = this.config.getString("dump.dir")

	def detailsInMemoryStorageLimit: Int = this.config.getInt("details.inmemory.storage.limit")

	private var _databaseDir: String = this.config.getString("database.dir")
	def databaseDir_=(v: String): Unit = {
		this._databaseDir = v
	}
	def databaseDir: String = Paths.get(this._databaseDir).toAbsolutePath.toString

	def genesisResourceName: String = this.config.getString("genesis")

	private var _databaseReset: Boolean = this.config.getBoolean("database.reset")
	def databaseReset_=(v: Boolean): Unit = {
		this._databaseReset = v
	}
	def databaseReset: Boolean = this._databaseReset

	//ブロック内のコード実行を行わない場合、blockchainOnlyとする。
	private var _blockchainOnly = this.config.getBoolean("blockchain.only")
	def blockchainOnly_=(v: Boolean): Unit = this._blockchainOnly = v
	def blockchainOnly: Boolean = this._blockchainOnly

	private var _recordBlocks = this.config.getBoolean("record.blocks")
	def recordBlocks_=(v: Boolean): Unit = this._recordBlocks = v
	def recordBlocks: Boolean = this._recordBlocks


	def cacheFlushMemory: Double = this.config.getDouble("cache.flush.memory")
	def cacheFlushBlocks: Int = this.config.getInt("cache.flush.blocks")

	def txOutdatedThreshold: Int = this.config.getInt("transaction.outdated.threshold")

	def networkId: Int = this.config.getInt("node.network.id")
	def myKey: ECKey = {
		val hex = this.config.getString("node.private.key")
		ECKey.fromPrivate(Hex.decodeHex(hex.toCharArray)).decompress
	}
	def nodeId: ImmutableBytes = ImmutableBytes(myKey.getNodeId)
	def externalAddress: String = this.config.getString("node.external.address")
	def bindAddress: String = this.config.getString("node.bind.address")
	def bindPort: Int = this.config.getInt("node.bind.port")

	def coinbaseSecret: String = this.config.getString("coinbase.secret")

	def helloPhrase: String = this.config.getString("hello.phrase")

	def isSyncEnabled: Boolean = this.config.getBoolean("sync.enabled")
	def maxHashesAsk: Int = this.config.getInt("sync.max.hashes.ask")
	def maxBlocksAsk: Int = this.config.getInt("sync.max.blocks.ask")
	def syncPeersCount: Int = this.config.getInt("sync.peer.count")

	def connectionTimeoutMillis: Int = this.config.getInt("node.connect.timeout.seconds") * 1000
	def readTimeoutMillis: Int = this.config.getInt("node.read.timeout.seconds") * 1000

	def isPublicHomeNode: Boolean = this.config.getBoolean("peer.discovery.public.home.node")

	def peerDiscoveryEnabled: Boolean = this.config.getBoolean("peer.discovery.enabled")
	def peerDiscoveryPersist: Boolean = this.config.getBoolean("peer.discovery.persist")
	def peerDiscoveryWorkers: Int = this.config.getInt("peer.discovery.workers")
	def peerDiscoveryAddresses: Seq[String] = {//TODO 名称。
		import scala.collection.JavaConversions._
		this.config.getStringList("peer.discovery.seed.nodes")
	}

	def peerDiscoveryTouchSeconds: Int = this.config.getInt("peer.discovery.touch.period")
	def peerDiscoveryTouchMaxNodes: Int = this.config.getInt("peer.discovery.touch.max.nodes")

	def blocksFile: String = this.config.getString("blocks.file")

}

object SystemProperties {

	private val configRef = new AtomicReference[SystemProperties](null)

	def loadFromFile(path: Path): SystemProperties = {
		this.synchronized {
			val config = ConfigFactory.parseFile(path.toFile)
			this.configRef.set(new SystemProperties(config))
			CONFIG
		}
	}

	def CONFIG: SystemProperties = this.configRef.get

}