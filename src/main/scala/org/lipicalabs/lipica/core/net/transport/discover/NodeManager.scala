package org.lipicalabs.lipica.core.net.transport.discover

import java.net.InetSocketAddress
import java.util
import java.util.Comparator
import java.util.concurrent.{TimeUnit, Executors}

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.db.datasource.mapdb.{MapDBFactoryImpl, MapDBFactory}
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.transport._
import org.lipicalabs.lipica.core.net.transport.discover.NodeStatistics.Persistent
import org.lipicalabs.lipica.core.net.transport.discover.table.NodeTable
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.{HTreeMap, DB}
import org.slf4j.LoggerFactory

import scala.collection.{mutable, JavaConversions}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 12:58
 * YANAGISAWA, Kentaro
 */
class NodeManager(val table: NodeTable, val key: ECKey) {
	import NodeManager._

	def worldManager: WorldManager = WorldManager.instance

	val peerConnectionManager: PeerConnectionManager = new PeerConnectionManager
	val mapDBFactory: MapDBFactory = new MapDBFactoryImpl

	private var _messageSender: MessageHandler = null
	private val nodeHandlerMap: mutable.Map[String, NodeHandler] = new mutable.HashMap[String, NodeHandler]

	val homeNode = this.table.node
	private var bootNodes: Seq[Node] = Seq.empty

	private val inboundOnlyFromKnownNodes: Boolean = true

	private val discoveryEnabled = SystemProperties.CONFIG.peerDiscoveryEnabled

	private val listeners: mutable.Map[DiscoverListener, ListenerHandler] = JavaConversions.mapAsScalaMap(new util.IdentityHashMap[DiscoverListener, ListenerHandler])

	private var _db: DB = null
	private var _nodeStatsDB: HTreeMap[Node, NodeStatistics.Persistent] = null
	private var _isInitDone = false

	def setBootNodes(aBootNodes: Seq[Node]): Unit = {
		this.bootNodes = aBootNodes
	}

	def channelActivated(): Unit = {
		if (this._isInitDone) {
			return
		}
		this._isInitDone = true
		val executor = Executors.newSingleThreadScheduledExecutor
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
		for (node <- this.bootNodes) {
			getNodeHandler(node)
		}
		for (node <- SystemProperties.CONFIG.activePeers) {
			getNodeHandler(node).nodeStatistics.setPredefined(true)
		}
	}

	private def dbRead(): Unit = {
		import JavaConversions._
		try {
			this._db = this.mapDBFactory.createTransactionalDB("network/discovery")
			if (SystemProperties.CONFIG.databaseReset) {
				this._db.delete("nodeStats")
			}
			this._nodeStatsDB = this._db.hashMap("nodeStats")
			val comparator = new Comparator[NodeStatistics.Persistent] {
				override def compare(o1: Persistent, o2: Persistent) = o2.reputation - o1.reputation
			}
			val sorted = this._nodeStatsDB.entrySet().toIndexedSeq.sortWith((e1, e2) => comparator.compare(e1.getValue, e2.getValue) <= 0)
			sorted.take(DbMaxLoadNodes).foreach {
				each => getNodeHandler(each.getKey).nodeStatistics.setPersistedData(each.getValue)
			}
		} catch {
			case e: Exception =>
				logger.warn("<NodeManager> Error reading from db.")
				try {
					this._db.delete("nodeStats")
					this._nodeStatsDB = this._db.hashMap("nodeStats")
				} catch {
					case ex: Exception =>
						logger.warn("<NodeManager> Error creating db.")
				}
		}
	}

	private def dbWrite(): Unit = {
		this.synchronized {
			for (handler <- this.nodeHandlerMap.values) {
				this._nodeStatsDB.put(handler.node, handler.nodeStatistics.getPersistentData)
			}
			this._db.commit()
			logger.info("<NodeManager> Node stats written to db: %,d".format(this._nodeStatsDB.size))
		}
	}

	def setMessageSender(v: MessageHandler): Unit = this._messageSender = v

	private def getKey(n: Node): String = getKey(new InetSocketAddress(n.host, n.port))

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
		val n = new Node(m.nodeId, sender.getHostName, sender.getPort)
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
			this._messageSender.accept(event)
		}
	}

	def stateChanged(nodeHandler: NodeHandler, oldState: NodeHandler.State, newState: NodeHandler.State): Unit = {
		this.peerConnectionManager.nodeStatusChanged(nodeHandler)
	}

	def getNodes(minReputation: Int): Seq[NodeHandler] = {
		this.synchronized {
			this.nodeHandlerMap.values.filter(each => minReputation <= each.nodeStatistics.reputation).toSeq
		}
	}

	def getBestLpcNodes(used: Set[String], lowerDifficulty: BigInt, limit: Int): Seq[NodeHandler] = {
		val predicate: (NodeHandler) => Boolean = (handler) => {
			if (handler.nodeStatistics.lpcTotalDifficulty eq null) {
				false
			} else if (used.contains(handler.node.hexId)) {
				false
			} else {
				lowerDifficulty < handler.nodeStatistics.lpcTotalDifficulty
			}
		}
		getNodes(predicate, BestDifficultyComparator, limit)
	}

	private def getNodes(predicate: (NodeHandler) => Boolean, comparator: Comparator[NodeHandler], limit: Int): Seq[NodeHandler] = {
		this.synchronized {
			this.nodeHandlerMap.values.toSeq.filter(predicate).sortWith((e1, e2) => comparator.compare(e1, e2) <= 0).take(limit)
		}
	}

	private def processListeners(): Unit = {
		this.synchronized {
			for (handler <- this.listeners.values) {
				try {
					handler.checkAll()
				} catch {
					case e: Exception =>
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
	private val logger = LoggerFactory.getLogger("discover")

	private val DummyStat = new NodeStatistics(new Node(ImmutableBytes.empty, "dummy.node", 0))
	private val Persist: Boolean = SystemProperties.CONFIG.peerDiscoveryPersist

	private val ListenerRefreshRate = 1000L
	private val DbCommitRate = 10000L
	private val DbMaxLoadNodes = 100

	def create: NodeManager = {
		val key = SystemProperties.CONFIG.myKey
		val homeNode = new Node(SystemProperties.CONFIG.nodeId, SystemProperties.CONFIG.externalAddress, SystemProperties.CONFIG.bindPort)
		val table = new NodeTable(homeNode, SystemProperties.CONFIG.isPublicHomeNode)
		new NodeManager(table, key)
	}



	private val BestDifficultyComparator = new Comparator[NodeHandler] {
		override def compare(n1: NodeHandler, n2: NodeHandler): Int = {
			val d1 = n1.nodeStatistics.lpcTotalDifficulty
			val d2 = n2.nodeStatistics.lpcTotalDifficulty
			d2.compare(d1)
		}
	}

}