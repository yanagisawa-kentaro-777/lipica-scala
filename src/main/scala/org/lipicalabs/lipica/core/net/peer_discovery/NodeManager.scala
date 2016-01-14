package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.{InetAddress, InetSocketAddress}
import java.util
import java.util.Comparator
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.concurrent.{ConcurrentHashMap, Executors, TimeUnit}

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.peer_discovery.NodeStatistics.Persistent
import org.lipicalabs.lipica.core.net.peer_discovery.discover._
import org.lipicalabs.lipica.core.net.peer_discovery.discover.table.NodeTable
import org.lipicalabs.lipica.core.net.peer_discovery.message.{FindNodeMessage, NeighborsMessage, PingMessage, PongMessage}
import org.lipicalabs.lipica.core.net.peer_discovery.udp.MessageHandler
import org.lipicalabs.lipica.core.utils.{ErrorLogger, CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.{JavaConversions, mutable}

/**
 * ピアディスカバリーにおける
 * 情報管理やメッセージ授受のハブになるクラスです。
 *
 * 自ノード全体で１インスタンスのみ生成されます。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/13 12:58
 * YANAGISAWA, Kentaro
 */
class NodeManager(val table: NodeTable, val key: ECKey, val dataSource: KeyValueDataSource) {
	import JavaConversions._
	import NodeManager._

	def worldManager: ComponentsMotherboard = ComponentsMotherboard.instance

	val peerConnectionExaminer: PeerConnectionExaminer = new PeerConnectionExaminer

	private val messageSenderRef: AtomicReference[MessageHandler] = new AtomicReference[MessageHandler](null)
	def messageSender_=(v: MessageHandler): Unit = this.messageSenderRef.set(v)
	def messageSender: MessageHandler = this.messageSenderRef.get

	private val nodeHandlerMap: mutable.Map[String, NodeHandler] = mapAsScalaConcurrentMap(new ConcurrentHashMap[String, NodeHandler])

	val homeNode = this.table.node
	private val seedNodesRef: AtomicReference[Seq[Node]] = new AtomicReference[Seq[Node]](Seq.empty)
	def seedNodes: Seq[Node] = this.seedNodesRef.get
	def seedNodes_=(v: Seq[Node]): Unit = this.seedNodesRef.set(v)

	private val inboundOnlyFromKnownNodes: Boolean = true

	private val discoveryEnabled = NodeProperties.CONFIG.peerDiscoveryEnabled

	private val listeners: mutable.Map[DiscoverListener, ListenerHandler] = JavaConversions.mapAsScalaMap(new util.IdentityHashMap[DiscoverListener, ListenerHandler])

//	private val dbRef: AtomicReference[DB] = new AtomicReference[DB](null)
//	private def db: DB = this.dbRef.get
//
//	private val nodeStatsDBRef: AtomicReference[HTreeMap[Node, NodeStatistics.Persistent]] = new AtomicReference[HTreeMap[Node, Persistent]](null)
//	private def nodeStatsDB: HTreeMap[Node, NodeStatistics.Persistent] = this.nodeStatsDBRef.get

	private val isInitDoneRef = new AtomicBoolean(false)
	def isInitDone: Boolean = this.isInitDoneRef.get

	def channelActivated(): Unit = {
		if (this.isInitDone) {
			return
		}
		this.isInitDoneRef.set(true)
		val executor = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("listener-processor"))
		executor.scheduleAtFixedRate(new Runnable {
			override def run(): Unit = {
				processListeners()
			}
		}, ListenerRefreshRate, ListenerRefreshRate, TimeUnit.MILLISECONDS)

		if (Persist) {
			dbRead()
			executor.scheduleAtFixedRate(new Runnable {
				override def run(): Unit = {
					dbWrite()
				}
			}, DbCommitRate, DbCommitRate, TimeUnit.MILLISECONDS)
		}
		for (node <- this.seedNodes) {
			getNodeHandler(node)
		}
		for (node <- NodeProperties.CONFIG.activePeers) {
			getNodeHandler(node).nodeStatistics.setPredefined(true)
		}
	}

	private def dbRead(): Unit = {
		try {
			if (NodeProperties.CONFIG.databaseReset) {
				this.dataSource.deleteAll()
			}
			val comparator = new Comparator[NodeStatistics.Persistent] {
				override def compare(o1: Persistent, o2: Persistent) = o2.reputation - o1.reputation
			}
			val loaded = this.dataSource.keys.flatMap(key => this.dataSource.get(key).map(value => (Node.decode(key), Persistent.decode(value))))
			//評判が高い順にソートする。
			val sorted = loaded.toSeq.sortWith((e1, e2) => comparator.compare(e1._2, e2._2) < 0)
			val (taken, dropped) = sorted.splitAt(DbMaxLoadNodes)
			//評判が高いものを大事にする。
			taken.foreach {
				each => getNodeHandler(each._1).nodeStatistics.setPersistedData(each._2)
			}
			//評判が低いものを消す。
			dropped.foreach(entry => this.dataSource.delete(entry._1.toEncodedBytes))
		} catch {
			case e: Exception =>
				ErrorLogger.logger.warn("<NodeManager> Error reading from data source.")
				logger.warn("<NodeManager> Error reading from data source.")
				try {
					this.dataSource.deleteAll()
				} catch {
					case ex: Exception =>
						ErrorLogger.logger.warn("<NodeManager> Error deleting data source.")
						logger.warn("<NodeManager> Error creating deleting data source.")
				}
		}
	}

	private def dbWrite(): Unit = {
		this.synchronized {
			val rows = new ArrayBuffer[(ImmutableBytes, ImmutableBytes)](this.nodeHandlerMap.size)
			for (handler <- this.nodeHandlerMap.values) {
				val key = handler.node.toEncodedBytes
				val value = handler.nodeStatistics.getPersistentData.encode
				rows.append((key, value))
			}
			this.dataSource.updateBatch(rows.toMap)
			logger.info("<NodeManager> Node stats written to data source: %,d".format(rows.size))
		}
	}

	private def getKey(n: Node): String = getKey(n.address)

	private def getKey(address: InetSocketAddress): String = {
		val addr = address.getAddress
		val host = if (addr eq null) address.getHostString else addr.getHostAddress
		host + ":" + address.getPort
	}

	def getNodeHandler(n: Node): NodeHandler = {
		this.synchronized {
			val key = getKey(n)
			this.nodeHandlerMap.get(key) match {
				case Some(result) =>
					result
				case None =>
					val result = new NodeHandler(n, this)
					this.nodeHandlerMap.put(key, result)
					if (logger.isDebugEnabled) {
						logger.debug("<NodeManager> New node: " + result)
					}
					this.worldManager.listener.onNodeDiscovered(result.node)
					result
			}
		}
	}

	def hasNodeHandler(n: Node): Boolean = this.nodeHandlerMap.contains(getKey(n))

	def getNodeStatistics(n: Node): NodeStatistics = {
		if (this.discoveryEnabled) {
			getNodeHandler(n).nodeStatistics
		} else {
			DummyStat
		}
	}

	def accept(discoveryEvent: DiscoveryEvent): Unit = handleInbound(discoveryEvent)

	def handleInbound(event: DiscoveryEvent): Unit = {
		val m = event.message
		val sender = event.address
		val n = new Node(m.nodeId, sender)

		if (this.inboundOnlyFromKnownNodes && !hasNodeHandler(n)) {
			logger.debug("<NodeManager> Inbound packet from unknown peer. Rejected.")
			return
		}
		val handler = getNodeHandler(n)
		m.messageType.head.toInt match {
			case 1 => handler.handlePing(m.asInstanceOf[PingMessage])
			case 2 => handler.handlePong(m.asInstanceOf[PongMessage])
			case 3 => handler.handleFindNode(m.asInstanceOf[FindNodeMessage])
			case 4 => handler.handleNeighbours(m.asInstanceOf[NeighborsMessage])
		}
	}

	def sendOutbound(event: DiscoveryEvent): Unit = {
		if (this.discoveryEnabled) {
			this.messageSender.accept(event)
		}
	}

	def stateChanged(nodeHandler: NodeHandler, oldState: NodeHandler.State, newState: NodeHandler.State): Unit = {
		this.peerConnectionExaminer.nodeStatusChanged(nodeHandler)
	}

	def getNodes(minReputation: Int): Seq[NodeHandler] = {
		this.synchronized {
			this.nodeHandlerMap.values.filter(each => minReputation <= each.nodeStatistics.reputation).toSeq
		}
	}

	def getBestLpcNodes(used: Set[ImmutableBytes], lowerDifficulty: BigInt, limit: Int): Seq[NodeHandler] = {
		val predicate: (NodeHandler) => Boolean = (handler) => {
			if (handler.nodeStatistics.lpcTotalDifficulty eq null) {
				false
			} else if (used.contains(handler.node.id)) {
				false
			} else {
				lowerDifficulty < handler.nodeStatistics.lpcTotalDifficulty
			}
		}
		getNodes(predicate, BestDifficultyComparator, limit)
	}

	private def getNodes(predicate: (NodeHandler) => Boolean, comparator: Comparator[NodeHandler], limit: Int): Seq[NodeHandler] = {
		this.synchronized {
			this.nodeHandlerMap.values.toSeq.filter(predicate).sortWith((e1, e2) => comparator.compare(e1, e2) < 0).take(limit)
		}
	}

	/**
	 * このインスタンスが知っているノードの数を返します。
	 */
	def numberOfKnownNodes: Int = {
		this.synchronized {
			this.nodeHandlerMap.size
		}
	}

	private def processListeners(): Unit = {
		this.synchronized {
			for (handler <- this.listeners.values) {
				try {
					handler.checkAll()
				} catch {
					case e: Exception =>
						ErrorLogger.logger.warn("<NodeManager> Error processing listener: " + handler, e)
						logger.warn("<NodeManager> Error processing listener: " + handler, e)
				}
			}
		}
	}

	def addDiscoveryListener(listener: DiscoverListener, predicate: (NodeStatistics) => Boolean): Unit = {
		this.synchronized {
			this.listeners.put(listener, new ListenerHandler(listener, predicate))
		}
	}

	def removeDiscoverListener(listener: DiscoverListener): Unit = {
		this.synchronized {
			this.listeners.remove(listener)
		}
	}

	class ListenerHandler(val listener: DiscoverListener, val predicate: (NodeStatistics) => Boolean) {
		private val discoveredNodes = JavaConversions.mapAsScalaMap(new util.IdentityHashMap[NodeHandler, NodeHandler])

		def checkAll(): Unit = {
			for (handler <- nodeHandlerMap.values) {
				val has = discoveredNodes.contains(handler)
				val ok = predicate(handler.nodeStatistics)

				if (!has && ok) {
					listener.nodeAppeared(handler)
					discoveredNodes.put(handler, handler)
				} else if (has && !ok) {
					listener.nodeDisappeared(handler)
					discoveredNodes.remove(handler)
				}
			}
		}
	}

}

object NodeManager {
	private val logger = LoggerFactory.getLogger("discovery")

	private val DummyStat = new NodeStatistics(new Node(ImmutableBytes.empty, new InetSocketAddress(InetAddress.getByAddress(new Array[Byte](4)), 0)))
	private val Persist: Boolean = NodeProperties.CONFIG.peerDiscoveryPersist

	private val ListenerRefreshRate = 1000L
	private val DbCommitRate = 10000L
	private val DbMaxLoadNodes = 100

	def create(dataSource: KeyValueDataSource): NodeManager = {
		val key = NodeProperties.CONFIG.privateKey
		val homeNodeAddress = new InetSocketAddress(NodeProperties.CONFIG.externalAddress, NodeProperties.CONFIG.bindPort)
		val homeNode = new Node(NodeProperties.CONFIG.nodeId, homeNodeAddress)
		val table = new NodeTable(homeNode, NodeProperties.CONFIG.isPublicHomeNode)
		new NodeManager(table, key, dataSource)
	}



	private val BestDifficultyComparator = new Comparator[NodeHandler] {
		override def compare(n1: NodeHandler, n2: NodeHandler): Int = {
			val d1 = n1.nodeStatistics.lpcTotalDifficulty
			val d2 = n2.nodeStatistics.lpcTotalDifficulty
			//TDの大きい方が順位が前になる。
			d2.compare(d1)
		}
	}

}