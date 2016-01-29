package org.lipicalabs.lipica.core.config

import java.io.InputStream
import java.net.{InetAddress, Socket, URI}
import java.nio.file.{Paths, Path}
import java.security.SecureRandom
import java.util.Properties
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.typesafe.config.{ConfigFactory, Config}
import org.apache.commons.codec.binary.Hex
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node}
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.lipicalabs.lipica.utils.{Version, MiscUtils}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 * このノードの設定情報を定義する trait です。
 */
trait NodePropertiesLike {

	/**
	 * このモジュールのバージョンを表す文字列を返します。
	 */
	def moduleVersion: Version

	/**
	 * 接続するネットワークの識別子を返します。
	 * （テスト用ネットワーク、本番用ネットワーク、等）
	 */
	def networkId: Int

	/**
	 * このノードの秘密鍵および公開鍵のペアを返します。
	 */
	def privateKey: ECKeyPair

	/**
	 * このノードの公開鍵から算出される、このノードのノードIDを返します。
	 */
	def nodeId: NodeId

	/**
	 * 外部のネットワーク（例えばインターネット）から
	 * このノードにアクセスするためのアドレスを返します。
	 */
	def externalAddress: InetAddress

	/**
	 * このノードがbindするアドレス（＝ローカルネットワークのアドレス）を返します。
	 */
	def bindAddress: InetAddress

	/**
	 * このノードがTCPおよびUDPにおいてbindするポート番号を返します。
	 */
	def bindPort: Int

	/**
	 * このノードが他のノードに対して提示する自ノードの情報に付加する文字列を返します。
	 */
	def helloPhrase: String

	/**
	 * ローカルファイルシステム上のデータストア用ディレクトリのパスを返します。
	 */
	def dataStoreDir: Path

	/**
	 * コントラクトにおいて、独立したデータストアを定義せずに
	 * ストレージ内のデータを保管する条件件数。
	 * （0でも良い。むしろ0がおすすめかも知れない。）
	 */
	def detailsInMemoryStorageLimit: Int

	/**
	 * データストアを削除するか否か。
	 * （実用性があまりないので削除を検討。）
	 */
	def shouldResetDataStore: Boolean

	/**
	 * genesisブロックの内容が記述されたリソースファイル名を返します。
	 * （テストと実稼働とで変えている。）
	 */
	def genesisResourceName: String

	/**
	 * トランザクションの実行や状態の整合性検査を行わない動作モードであるか否かを返します。
	 * （デバッグ等の便宜用。）
	 */
	def blockchainOnly: Boolean

	/**
	 * ブロックのシリアライズされた表現を、専用のログファイルに出力するか否かを返します。
	 */
	def recordBlocks: Boolean

	/**
	 * メモリ上に溜められた更新情報を、
	 * 永続化用の領域にフラッシュするトリガーとなるメモリ消費率を返します。
	 */
	def cacheFlushMemory: Double

	/**
	 * メモリ上に溜められた更新情報を、
	 * 永続化用の領域にフラッシュするトリガーとなる処理ブロック数を返します。
	 */
	def cacheFlushBlocks: Int

	/**
	 * 通信における接続タイムアウトミリ秒を返します。
	 */
	def connectionTimeoutMillis: Int

	/**
	 * 通信における読み取りタイムアウトミリ秒を返します。
	 */
	def readTimeoutMillis: Int

	/**
	 * ネットワークからブロックを取得して
	 * 自ノードのブロックチェーンに同期させる処理を行うか否かを返します。
	 */
	def isSyncEnabled: Boolean

	/**
	 * ピアに対して一度に要求するブロックハッシュ値の数を返します。
	 */
	def maxHashesAsk: Int

	/**
	 * ピアに対して一度に要求するブロックの数を返します。
	 */
	def maxBlocksAsk: Int

	/**
	 * 同期処理において通信するノードの数を返します。
	 */
	def syncPeersCount: Int

	/**
	 * 自ノードの情報をKADEMLIAのテーブルに含めて
	 * 他ノードに公開するか否かを返します。
	 */
	def isPublicHomeNode: Boolean

	/**
	 * 自動的なピアの発見を行うか否かを返します。
	 */
	def peerDiscoveryEnabled: Boolean

	/**
	 * 発見されたピアの情報を永続化するか否かを返します。
	 */
	def peerDiscoveryPersist: Boolean

	/**
	 * ピアの発見処理を行うスレッドの数を返します。
	 */
	def peerDiscoveryWorkers: Int

	/**
	 * ネットワークに参加する起点となるシードノードの並びを返します。
	 */
	def seedNodes: Seq[URI]

	/**
	 * ピアディスカバリーにおいて、ノードの情報を更新する間隔（秒）。
	 */
	def peerDiscoveryTouchSeconds: Int

	/**
	 * ピアディスカバリーにおいて、ノードの情報を更新する対象数。
	 */
	def peerDiscoveryTouchMaxNodes: Int

	/**
	 * ブロックをネットワークから取得するのではなく
	 * ローカルのファイルから読み取る場合の、
	 * ファイルが配置されているディレクトリのパスを返します。
	 */
	def srcBlocksDir: Option[Path]

	/**
	 * 保留中トランザクションの時間切れ判定の基準とするブロック番号の差を返します。
	 */
	def txOutdatedThreshold: Int

	/**
	 * このノードにおいて、REST APIサーバーを有効化するか否かを返します。
	 */
	def restApiEnabled: Boolean

	/**
	 * REST APIにおけるbindアドレスを返します。
	 */
	def restApiBindAddress: InetAddress

	/**
	 * REST APIにおけるbindポート番号を返します。
	 */
	def restApiBindPort: Int

	/**
	 * ブロック報酬を変えるフラグ。
	 * （ユニットテスト用と実稼働用途で値を変えている。）
	 */
	def isFrontier: Boolean

	/**
	 * ブロックチェーンに連結するブロックの中身を、特定のログファイルに出力するかを返します。
	 * @return
	 */
	def dumpBlock: Long

	def dumpDir: String

	def activePeers: Seq[Node]

	def vmTrace: Boolean

	def vmTraceInitStorageLimit: Int


	//TODO 痕跡的。不要。
	def coinbaseSecret: String

}

/**
 * Configオブジェクトに基いて設定値を供給する実装です。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class NodeProperties(val config: Config) extends NodePropertiesLike {

	import NodeProperties._

	private val isFrontierRef = new AtomicBoolean(true)
	override def isFrontier: Boolean = this.isFrontierRef.get

	override def moduleVersion: Version = {
		val properties = new Properties()
		withSystemResource[Unit]("lipica_version.properties") {
			in => properties.load(in)
		}
		val version = properties.getProperty("version")
		val modifier = properties.getProperty("modifier")
		val build = properties.getProperty("build")

		Version.parse(version, option(modifier), option(build)).right.get
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

	private val dataStoreDirRef: AtomicReference[Path] = new AtomicReference(Paths.get(this.config.getString("datastore.dir")).toAbsolutePath)
	override def dataStoreDir: Path = this.dataStoreDirRef.get

	private val resetDataStoreRef: AtomicBoolean = new AtomicBoolean(this.config.getBoolean("datastore.reset"))
	override def shouldResetDataStore: Boolean = this.resetDataStoreRef.get

	override def genesisResourceName: String = this.config.getString("genesis")

	//ブロック内のコード実行を行わない場合、blockchainOnlyとする。
	private val blockchainOnlyRef: AtomicBoolean = new AtomicBoolean(this.config.getBoolean("blockchain.only"))
	def blockchainOnly_=(v: Boolean): Unit = this.blockchainOnlyRef.set(v)
	override def blockchainOnly: Boolean = this.blockchainOnlyRef.get

	//取得したブロックを、ブロックチェーンに連結する際にファイルにも出力するか否か。
	private val recordBlocksRef: AtomicBoolean = new AtomicBoolean(this.config.getBoolean("record.blocks"))
	def recordBlocks_=(v: Boolean): Unit = this.recordBlocksRef.set(v)
	override def recordBlocks: Boolean = this.recordBlocksRef.get

	override def cacheFlushMemory: Double = this.config.getDouble("cache.flush.memory")
	override def cacheFlushBlocks: Int = this.config.getInt("cache.flush.blocks")

	override def txOutdatedThreshold: Int = this.config.getInt("transaction.outdated.threshold")

	/**
	 * 自ノードが属すべき「ネットワーク」の識別子です。
	 * 「ネットワーク」とは、テスト用空間、本番用空間等を区別するためのものです。
	 */
	override def networkId: Int = this.config.getInt("node.network.id")

	private val privateKeyRef = new AtomicReference[ECKeyPair](null)
	/**
	 * 自ノードの秘密鍵です。
	 */
	@tailrec
	override final def privateKey: ECKeyPair = {
		val result = this.privateKeyRef.get
		if (result ne null) {
			return result
		}
		this.privateKeyRef.synchronized {
			val key = "node.private.key"
			val hex =
				if (config.hasPath(key)) {
					config.getString(key)
				} else {
					""
				}
			val keyBytes =
				if (MiscUtils.isNullOrEmpty(hex, trim = true) || (hex.length < 64)) {
					//指定されていないのでランダムに生成する。
					val random = new SecureRandom()
					val bytes = new Array[Byte](32)
					random.nextBytes(bytes)
					bytes
				} else {
					Hex.decodeHex(hex.toCharArray)
				}
			this.privateKeyRef.set(ECKeyPair.fromPrivateKey(keyBytes).decompress)
		}
		//再帰的自己呼び出し。
		this.privateKey
	}

	/**
	 * 「ネットワーク」内における自ノードの一意識別子です。
	 * その内容は、自ノードの秘密鍵に対応する公開鍵です。
	 */
	override def nodeId: NodeId = privateKey.toNodeId

	/**
	 * 他ノードに対して宣伝する、自ノードの体外部アドレスです。
	 * （典型的には、他ノードがインターネット経由で自ノードに接続しようとする際に利用。）
	 */
	private val externalAddressRef: AtomicReference[InetAddress] = new AtomicReference[InetAddress](null)
	override def externalAddress: InetAddress = {
		val result = this.externalAddressRef.get
		if (result ne null) {
			result
		} else {
			this.synchronized {
				val key = "node.external.address"
				val address: InetAddress =
					if (this.config.hasPath(key)) {
						//優先候補：直接設定。
						InetAddress.getByName(this.config.getString(key))
					} else {
						//次善候補：外部サービスに訊いてみる。
						httpGet(CheckAddressUri).map(s => InetAddress.getByName(s)).getOrElse {
							//最終候補：bindアドレス。
							bindAddress
						}
					}
				this.externalAddressRef.set(address)
				logger.info("<SystemProperties> External address of this node is set to: %s".format(address))
				address
			}
		}
	}

	private val bindAddressRef: AtomicReference[InetAddress] = new AtomicReference[InetAddress](null)
	override def bindAddress: InetAddress = {
		val result = this.bindAddressRef.get
		if (result ne null) {
			result
		} else {
			this.synchronized {
				val key = "node.bind.address"
				val address =
					if (this.config.hasPath(key)) {
						//優先候補：直接設定。
						InetAddress.getByName(this.config.getString(key))
					} else {
						//次善候補：外部サービスに訊いてみる。
						val socket = new Socket("www.google.com", 80)
						try {
							val address = socket.getLocalAddress
							if (address ne null) {
								address
							} else {
								InetAddress.getByName("0.0.0.0")
							}
						} finally {
							MiscUtils.closeIfNotNull(socket)
						}
					}
				this.bindAddressRef.set(address)
				logger.info("<SystemProperties> Bind address of this node is set to: %s".format(address))
				address
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
	override def seedNodes: Seq[URI] = {
		import scala.collection.JavaConversions._
		val strings = this.config.getStringList("peer.discovery.seed.nodes")
		strings.map(s => URI.create(s.trim))
	}

	override def peerDiscoveryTouchSeconds: Int = this.config.getInt("peer.discovery.touch.period")
	override def peerDiscoveryTouchMaxNodes: Int = this.config.getInt("peer.discovery.touch.max.nodes")
	override def srcBlocksDir: Option[Path] = {
		val s = getOrElse(this.config, "src.blocks.dir", "")
		if (s.nonEmpty) {
			Option(Paths.get(s.trim).toAbsolutePath)
		} else {
			None
		}
	}

	override def restApiEnabled: Boolean = this.config.getBoolean("api.rest.enabled")
	override def restApiBindAddress: InetAddress = {
		val s = this.config.getString("api.rest.bind.address")
		if (!MiscUtils.isNullOrEmpty(s, trim = true)) {
			InetAddress.getByName(s)
		} else {
			InetAddress.getByName("0.0.0.0")
		}
	}
	override def restApiBindPort: Int = this.config.getInt("api.rest.bind.port")

}

/**
 * ユニットテスト用のダミー設定クラスです。
 */
object DummyNodeProperties$ extends NodePropertiesLike {

	override def dumpDir: String = "./work/dump"

	override def moduleVersion: Version = Version.zero

	override def peerDiscoveryWorkers: Int = 8

	override def vmTraceInitStorageLimit: Int = 1000

	override def bindPort: Int = 30300

	override def coinbaseSecret: String = "secret"

	override def vmTrace: Boolean = false

	override def dataStoreDir: Path = Paths.get("./work/database/").toAbsolutePath

	override def activePeers: Seq[Node] = Seq.empty

	override def isPublicHomeNode: Boolean = true

	override def nodeId: NodeId = privateKey.toNodeId

	override def cacheFlushMemory: Double = 0.7d

	override def dumpBlock: Long = -1L

	override def peerDiscoveryPersist: Boolean = false

	override def isSyncEnabled: Boolean = true

	override def privateKey: ECKeyPair = {
		ECKeyPair.fromPrivateKey(Hex.decodeHex("a43d867f16238b897428705cec855b0c5b0ddf3319c1b18f7a00915db83155d9".toCharArray)).decompress
	}

	override def externalAddress: InetAddress = InetAddress.getByName("127.0.0.1")

	override def maxHashesAsk: Int = 10000

	override def recordBlocks: Boolean = false

	override def peerDiscoveryTouchSeconds: Int = 10000

	override def txOutdatedThreshold: Int = 10

	override def readTimeoutMillis: Int = 10 * 1000

	override def networkId: Int = 1

	override def srcBlocksDir: Option[Path] = None

	override def isFrontier: Boolean = false

	override def peerDiscoveryTouchMaxNodes: Int = 10000

	override def connectionTimeoutMillis: Int = 10 * 1000

	override def peerDiscoveryEnabled: Boolean = true

	override def bindAddress: InetAddress = InetAddress.getByName("0.0.0.0")

	override def cacheFlushBlocks: Int = 10000

	override def seedNodes: Seq[URI] = Seq.empty

	override def detailsInMemoryStorageLimit: Int = 100

	override def blockchainOnly: Boolean = false

	override def shouldResetDataStore: Boolean = false

	override def maxBlocksAsk: Int = 10000

	override def syncPeersCount: Int = 8

	override def genesisResourceName: String = "genesis1.json"

	override def helloPhrase: String = "hello"

	override def restApiEnabled: Boolean = false
	override def restApiBindAddress: InetAddress = InetAddress.getByName("0.0.0.0")
	override def restApiBindPort: Int = 0
}

object NodeProperties {
	private val logger = LoggerFactory.getLogger("general")

	private val CheckAddressUri: String = "http://checkip.amazonaws.com"

	private val configRef = new AtomicReference[NodePropertiesLike](null)

	def loadFromFile(path: Path): NodeProperties = {
		this.synchronized {
			val config = ConfigFactory.parseFile(path.toAbsolutePath.toFile)
			val result = new NodeProperties(config)
			this.configRef.set(result)
			result
		}
	}

	def CONFIG: NodePropertiesLike = {
		val result = this.configRef.get
		if (result eq null) {
			//ユニットテスト用。
			//TODO もう少しマシなやり方は？
			DummyNodeProperties$
		} else {
			//通常ルート。
			result
		}
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
				ErrorLogger.logger.warn("<SystemProperties> Failed to detect ip address by %s".format(uri))
				logger.warn("<SystemProperties> Failed to detect ip address by %s".format(uri))
				None
		} finally {
			MiscUtils.closeIfNotNull(httpClient)
		}
	}

	private def getOrElse(config: Config, key: String, value: String): String = {
		if (config.hasPath(key)) {
			config.getString(key).trim
		} else {
			value
		}
	}

	private def option(s: String): Option[String] = {
		if (MiscUtils.isNullOrEmpty(s, trim = true)) {
			None
		} else {
			Some(s.trim)
		}
	}

}
