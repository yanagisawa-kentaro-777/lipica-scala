package org.lipicalabs.lipica.core.config

import java.nio.file.{Paths, Path}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes

trait SystemPropertiesLike {
	def isStorageDictionaryEnabled: Boolean
	def isFrontier: Boolean

	def projectVersion: String

	def activePeers: Seq[Node]


	def vmTrace: Boolean
	def vmTraceInitStorageLimit: Int
	def dumpBlock: Long
	def dumpDir: String

	def detailsInMemoryStorageLimit: Int

	def databaseDir: String
	def databaseDir_=(v: String): Unit

	def genesisResourceName: String

	def databaseReset_=(v: Boolean): Unit
	def databaseReset: Boolean

	def blockchainOnly: Boolean

	def recordBlocks: Boolean


	def cacheFlushMemory: Double
	def cacheFlushBlocks: Int

	def txOutdatedThreshold: Int

	def networkId: Int
	def myKey: ECKey
	def nodeId: ImmutableBytes
	def externalAddress: String
	def bindAddress: String
	def bindPort: Int

	def coinbaseSecret: String

	def helloPhrase: String

	def isSyncEnabled: Boolean
	def maxHashesAsk: Int
	def maxBlocksAsk: Int
	def syncPeersCount: Int

	def connectionTimeoutMillis: Int
	def readTimeoutMillis: Int

	def isPublicHomeNode: Boolean

	def peerDiscoveryEnabled: Boolean
	def peerDiscoveryPersist: Boolean
	def peerDiscoveryWorkers: Int
	def seedNodes: Seq[String]

	def peerDiscoveryTouchSeconds: Int
	def peerDiscoveryTouchMaxNodes: Int

	def blocksFile: String

}

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class SystemProperties(val config: Config) extends SystemPropertiesLike {

	def isStorageDictionaryEnabled: Boolean = {
		//TODO
		false
	}

	private val isFrontierRef = new AtomicBoolean(true)
	def isFrontier: Boolean = this.isFrontierRef.get

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
	def seedNodes: Seq[String] = {
		import scala.collection.JavaConversions._
		this.config.getStringList("peer.discovery.seed.nodes")
	}

	def peerDiscoveryTouchSeconds: Int = this.config.getInt("peer.discovery.touch.period")
	def peerDiscoveryTouchMaxNodes: Int = this.config.getInt("peer.discovery.touch.max.nodes")

	def blocksFile: String = this.config.getString("blocks.file")

}

class DummySystemProperties extends SystemPropertiesLike {

	override def isStorageDictionaryEnabled: Boolean = false

	override def dumpDir: String = "dump"

	override def projectVersion: String = "0.5"

	override def peerDiscoveryWorkers: Int = 8

	override def vmTraceInitStorageLimit: Int = 1000

	override def bindPort: Int = 30300

	override def coinbaseSecret: String = "secret"

	override def vmTrace: Boolean = false

	private var _databaseDir: String = "./worl/database/"
	def databaseDir_=(v: String): Unit = {
		this._databaseDir = v
	}
	def databaseDir: String = Paths.get(this._databaseDir).toAbsolutePath.toString

	override def activePeers: Seq[Node] = Seq.empty

	override def isPublicHomeNode: Boolean = true

	override def nodeId: ImmutableBytes = ImmutableBytes(myKey.getNodeId)

	override def cacheFlushMemory: Double = 0.7d

	override def dumpBlock: Long = -1L

	override def peerDiscoveryPersist: Boolean = false

	override def isSyncEnabled: Boolean = true

	override def myKey: ECKey = {
		ECKey.fromPrivate(Hex.decodeHex("a43d867f16238b897428705cec855b0c5b0ddf3319c1b18f7a00915db83155d9".toCharArray)).decompress
	}

	override def externalAddress: String = "127.0.0.1"

	override def maxHashesAsk: Int = 10000

	override def recordBlocks: Boolean = false

	override def peerDiscoveryTouchSeconds: Int = 10000

	override def txOutdatedThreshold: Int = 10

	override def readTimeoutMillis: Int = 10 * 1000

	override def networkId: Int = 1

	override def blocksFile: String = ""

	override def isFrontier: Boolean = false

	override def peerDiscoveryTouchMaxNodes: Int = 10000

	override def connectionTimeoutMillis: Int = 10 * 1000

	override def peerDiscoveryEnabled: Boolean = true

	override def bindAddress: String = "0.0.0.0"

	override def cacheFlushBlocks: Int = 10000

	override def seedNodes: Seq[String] = Seq.empty

	override def detailsInMemoryStorageLimit: Int = 10000

	override def blockchainOnly: Boolean = false

	private var _databaseReset: Boolean = false
	def databaseReset_=(v: Boolean): Unit = {
		this._databaseReset = v
	}
	def databaseReset: Boolean = this._databaseReset

	override def maxBlocksAsk: Int = 10000

	override def syncPeersCount: Int = 8

	override def genesisResourceName: String = "genesis1.json"

	override def helloPhrase: String = "hello"
}

object SystemProperties {

	private val configRef = new AtomicReference[SystemPropertiesLike](null)

	def loadFromFile(path: Path): SystemProperties = {
		this.synchronized {
			val config = ConfigFactory.parseFile(path.toAbsolutePath.toFile)
			val result = new SystemProperties(config)
			this.configRef.set(result)
			result
		}
	}

	def CONFIG: SystemPropertiesLike = {
		val result = this.configRef.get
		if (result eq null) {
			//ユニットテスト用。
			//TODO もう少しマシなやり方。
			val r = new DummySystemProperties
			this.configRef.set(r)
			r
		} else {
			//通常ルート。
			result
		}
	}

}