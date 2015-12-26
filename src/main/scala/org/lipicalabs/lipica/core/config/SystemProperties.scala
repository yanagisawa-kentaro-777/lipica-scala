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

	def vmTrace: Boolean = {
		//TODO
		true
	}

	def vmTraceInitStorageLimit: Int = {
		//TODO
		1000000
	}

	def isStorageDictionaryEnabled: Boolean = {
		//TODO
		false
	}

	def dumpBlock: Long = {
		//TODO
		-1L
	}

	def detailsInMemoryStorageLimit: Int = {
		//TODO
		1000
	}

	def isFrontier: Boolean = {
		//TODO
		true
	}

	private var _databaseDir: String = this.config.getString("database.dir")
	def databaseDir_=(v: String): Unit = {
		this._databaseDir = v
	}
	def databaseDir: String = Paths.get(this._databaseDir).toAbsolutePath.toString

	def genesisInfo: String = this.config.getString("genesis")

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

	def dumpDir: String = {
		//TODO
		"dump"
	}

	def cacheFlushMemory: Double = 0d
	def cacheFlushBlocks: Int = 0

	def txOutdatedThreshold: Int = 3

	def externalAddress: String = this.config.getString("node.external.address")

	def bindAddress: String = this.config.getString("node.bind.address")

	def bindPort: Int = this.config.getInt("node.bind.port")

	def projectVersion: String = {
		//TODO
		"0.5.0.0"
	}

	def helloPhrase: String = this.config.getString("hello.phrase")

	def maxHashesAsk: Int = {
		//TODO
		10000
	}

	def maxBlocksAsk: Int = {
		//TODO
		100
	}

	def networkId: Int = {
		//TODO
		1
	}

	def isSyncEnabled: Boolean = {
		//TODO
		true
	}

	def activePeers: Seq[Node] = {
		//TODO
		Seq.empty
	}

	def syncPeersCount: Int = {
		//TODO
		8
	}

	def peerConnectionTimeoutMillis: Int = {
		//TODO
		10000
	}

	def peerChannelReadTimeoutSeconds: Int = {
		//TODO
		10
	}

	def peerDiscoveryEnabled: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryPersist: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryWorkers: Int = {
		//TODO
		8
	}

	def peerDiscoveryAddresses: Seq[String] = {
		import scala.collection.JavaConversions._
		this.config.getStringList("peer.discovery.seed.nodes")
	}

	def myKey: ECKey = {
		val hex = this.config.getString("node.private.key")
		ECKey.fromPrivate(Hex.decodeHex(hex.toCharArray)).decompress
	}

	def coinbaseSecret: String = {
		//TODO
		"secret"
	}

	def nodeId: ImmutableBytes = ImmutableBytes(myKey.getNodeId)

	def isPublicHomeNode: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryTouchSeconds: Int = {
		//TODO
		600
	}

	def peerDiscoveryTouchMaxNodes: Int = {
		//TODO
		100
	}

	def blocksFile: String = {
		//TODO
		""
	}

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