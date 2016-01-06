package org.lipicalabs.lipica.core.config

import java.io.InputStream
import java.net.{Socket, URI}
import java.nio.file.{Paths, Path}
import java.security.SecureRandom
import java.util.Properties
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.commons.codec.binary.Hex
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 * このシステムの設定を表すインターフェイスです。
 */
trait SystemPropertiesLike {

	def isFrontier: Boolean

	def moduleVersion: String

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

	def restApiEnabled: Boolean
	def restApiBindAddress: String
	def restApiBindPort: Int

}

/**
 * Configオブジェクトに基いて設定値を供給する実装です。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class SystemProperties(val config: Config) extends SystemPropertiesLike {

	import SystemProperties._

	private val isFrontierRef = new AtomicBoolean(true)
	override def isFrontier: Boolean = this.isFrontierRef.get

	override def moduleVersion: String = {
		val properties = new Properties()
		withSystemResource[Unit]("version.properties") {
			in => properties.load(in)
		}
		val version = properties.getProperty("version")
		val modifier = properties.getProperty("modifier")

		if (SystemProperties.isNullOrEmpty(modifier)) {
			version
		} else {
			version + "-" + modifier
		}
	}

	override def activePeers: Seq[Node] = {
		import scala.collection.JavaConversions._
		val key = "active.peers"
		if (this.config.hasPath(key)) {
			this.config.getStringList(key).map {
				each => Node(new URI(each))
			}
		} else {
			Seq.empty
		}
	}

	override def vmTrace: Boolean = this.config.getBoolean("vm.structured.trace")
	override def vmTraceInitStorageLimit: Int = this.config.getInt("vm.structured.init.storage.limit")

	override def dumpBlock: Long = this.config.getLong("dump.block")
	override def dumpDir: String = this.config.getString("dump.dir")

	override def detailsInMemoryStorageLimit: Int = this.config.getInt("details.inmemory.storage.limit")

	private val databaseDirRef: AtomicReference[String] = new AtomicReference(this.config.getString("database.dir"))
	override def databaseDir_=(v: String): Unit = {
		this.databaseDirRef.set(v)
	}
	override def databaseDir: String = Paths.get(this.databaseDirRef.get).toAbsolutePath.toString

	override def genesisResourceName: String = this.config.getString("genesis")

	private val databaseResetRef: AtomicBoolean = new AtomicBoolean(this.config.getBoolean("database.reset"))
	override def databaseReset_=(v: Boolean): Unit = {
		this.databaseResetRef.set(v)
	}
	override def databaseReset: Boolean = this.databaseResetRef.get

	//ブロック内のコード実行を行わない場合、blockchainOnlyとする。
	private val blockchainOnlyRef: AtomicBoolean = new AtomicBoolean(this.config.getBoolean("blockchain.only"))
	def blockchainOnly_=(v: Boolean): Unit = this.blockchainOnlyRef.set(v)
	override def blockchainOnly: Boolean = this.blockchainOnlyRef.get

	private var _recordBlocks = this.config.getBoolean("record.blocks")
	def recordBlocks_=(v: Boolean): Unit = this._recordBlocks = v
	override def recordBlocks: Boolean = this._recordBlocks

	override def cacheFlushMemory: Double = this.config.getDouble("cache.flush.memory")
	override def cacheFlushBlocks: Int = this.config.getInt("cache.flush.blocks")

	override def txOutdatedThreshold: Int = this.config.getInt("transaction.outdated.threshold")

	/**
	 * 自ノードが属すべき「ネットワーク」の識別子です。
	 * 「ネットワーク」とは、テスト用空間、本番用空間等を区別するためのものです。
	 */
	override def networkId: Int = this.config.getInt("node.network.id")

	private val privateKeyRef = new AtomicReference[ECKey](null)
	/**
	 * 自ノードの秘密鍵です。
	 */
	@tailrec
	override final def myKey: ECKey = {
		val result = this.privateKeyRef.get
		if (result ne null) {
			return result
		}
		this.privateKeyRef.synchronized {
			val hex = this.config.getString("node.private.key")
			val keyBytes =
				if (isNullOrEmpty(hex) || (hex.length < 64)) {
					//指定されていないのでランダムに生成する。
					val random = new SecureRandom()
					val bytes = new Array[Byte](32)
					random.nextBytes(bytes)
					bytes
				} else {
					Hex.decodeHex(hex.toCharArray)
				}
			val privateKey = ECKey.fromPrivate(keyBytes).decompress
			this.privateKeyRef.set(privateKey)
		}
		myKey
	}

	/**
	 * 「ネットワーク」内における自ノードの一意識別子です。
	 * その内容は、自ノードの秘密鍵に対応する公開鍵です。
	 */
	override def nodeId: ImmutableBytes = ImmutableBytes(myKey.getNodeId)

	/**
	 * 他ノードに対して宣伝する、自ノードの体外部アドレスです。
	 * （典型的には、他ノードがインターネット経由で自ノードに接続しようとする際に利用。）
	 */
	private val externalAddressRef: AtomicReference[String] = new AtomicReference[String](null)
	override def externalAddress: String = {
		val result = this.externalAddressRef.get
		if (!isNullOrEmpty(result)) {
			result
		} else {
			this.synchronized {
				val key = "node.external.address"
				val candidate =
					if (this.config.hasPath(key)) {
						//優先候補：直接設定。
						this.config.getString(key)
					} else {
						//次善候補：外部サービスに訊いてみる。
						httpGet(CheckAddressUri).getOrElse {
							//最終候補：bindアドレス。
							bindAddress
						}
					}
				this.externalAddressRef.set(candidate)
				logger.info("<SystemProperties> External address of this node is set to: %s".format(candidate))
				candidate
			}
		}
	}

	private val bindAddressRef: AtomicReference[String] = new AtomicReference[String](null)
	override def bindAddress: String = {
		val result = this.bindAddressRef.get
		if (!isNullOrEmpty(result)) {
			result
		} else {
			this.synchronized {
				val key = "node.bind.address"
				val candidate =
					if (this.config.hasPath(key)) {
						//優先候補：直接設定。
						this.config.getString(key)
					} else {
						//次善候補：外部サービスに訊いてみる。
						val socket = new Socket("www.google.com", 80)
						val a = socket.getLocalAddress.getHostAddress
						if (!isNullOrEmpty(a)) {
							a
						} else {
							"0.0.0.0"
						}
					}
				this.bindAddressRef.set(candidate)
				logger.info("<SystemProperties> Bind address of this node is set to: %s".format(candidate))
				candidate
			}
		}
	}
	override def bindPort: Int = this.config.getInt("node.bind.port")

	override def coinbaseSecret: String = this.config.getString("coinbase.secret")

	override def helloPhrase: String = this.config.getString("hello.phrase")

	override def isSyncEnabled: Boolean = this.config.getBoolean("sync.enabled")
	override def maxHashesAsk: Int = this.config.getInt("sync.max.hashes.ask")
	override def maxBlocksAsk: Int = this.config.getInt("sync.max.blocks.ask")
	override def syncPeersCount: Int = this.config.getInt("sync.peer.count")

	override def connectionTimeoutMillis: Int = this.config.getInt("node.connect.timeout.seconds") * 1000
	override def readTimeoutMillis: Int = this.config.getInt("node.read.timeout.seconds") * 1000

	override def isPublicHomeNode: Boolean = this.config.getBoolean("peer.discovery.public.home.node")

	override def peerDiscoveryEnabled: Boolean = this.config.getBoolean("peer.discovery.enabled")
	override def peerDiscoveryPersist: Boolean = this.config.getBoolean("peer.discovery.persist")
	override def peerDiscoveryWorkers: Int = this.config.getInt("peer.discovery.workers")
	override def seedNodes: Seq[String] = {
		import scala.collection.JavaConversions._
		this.config.getStringList("peer.discovery.seed.nodes")
	}

	override def peerDiscoveryTouchSeconds: Int = this.config.getInt("peer.discovery.touch.period")
	override def peerDiscoveryTouchMaxNodes: Int = this.config.getInt("peer.discovery.touch.max.nodes")
	override def blocksFile: String = this.config.getString("blocks.file")

	override def restApiEnabled: Boolean = this.config.getBoolean("api.rest.enabled")
	override def restApiBindAddress: String = this.config.getString("api.rest.bind.address")
	override def restApiBindPort: Int = this.config.getInt("api.rest.bind.port")

}

/**
 * ユニットテスト用のダミー設定クラスです。
 */
object DummySystemProperties extends SystemPropertiesLike {

	override def dumpDir: String = "./work/dump"

	override def moduleVersion: String = "0.5.0"

	override def peerDiscoveryWorkers: Int = 8

	override def vmTraceInitStorageLimit: Int = 1000

	override def bindPort: Int = 30300

	override def coinbaseSecret: String = "secret"

	override def vmTrace: Boolean = false

	private var _databaseDir: String = "./work/database/"

	override def databaseDir_=(v: String): Unit = {
		this._databaseDir = v
	}

	override def databaseDir: String = Paths.get(this._databaseDir).toAbsolutePath.toString

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

	override def databaseReset_=(v: Boolean): Unit = {
		this._databaseReset = v
	}

	override def databaseReset: Boolean = this._databaseReset

	override def maxBlocksAsk: Int = 10000

	override def syncPeersCount: Int = 8

	override def genesisResourceName: String = "genesis1.json"

	override def helloPhrase: String = "hello"

	override def restApiEnabled: Boolean = false
	override def restApiBindAddress: String = ""
	override def restApiBindPort: Int = 0
}

object SystemProperties {
	private val logger = LoggerFactory.getLogger("general")

	private val CheckAddressUri: String = "http://checkip.amazonaws.com"

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
			//TODO もう少しマシなやり方は？
			DummySystemProperties
		} else {
			//通常ルート。
			result
		}
	}

	private def isNullOrEmpty(s: String): Boolean = {
		if (s eq null) {
			return true
		}
		val trimmed = s.trim
		trimmed.isEmpty || (trimmed.toLowerCase == "null")
	}

	private def withSystemResource[T](resourceName: String)(proc: (InputStream) => T): T = {
		val in = ClassLoader.getSystemResourceAsStream(resourceName)
		try {
			proc(in)
		} finally {
			in.close()
		}
	}

	private def httpGet(uri: String): Option[String] = {
		val httpClient = HttpClients.createDefault
		try {
			val httpGet = new HttpGet(uri)
			val resp = httpClient.execute(httpGet)
			try {
				val statusCode = resp.getStatusLine.getStatusCode
				if ((200 <= statusCode) && (statusCode < 300)) {
					Option(EntityUtils.toString(resp.getEntity).trim)
				} else {
					None
				}
			} finally {
				resp.close()
			}
		} catch {
			case e: Throwable =>
				logger.warn("<SystemProperties> Failed to detect ip address by %s".format(uri))
				None
		} finally {
			httpClient.close()
		}
	}

}
