package org.lipicalabs.lipica.core.net.peer_discovery.active_discovery

import java.net.{InetSocketAddress, InetAddress, URI, UnknownHostException}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.net.p2p.Peer
import org.lipicalabs.lipica.core.net.peer_discovery.PeerInfo
import org.lipicalabs.lipica.core.utils.{ErrorLogger, CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:55
 * YANAGISAWA, Kentaro
 */
class PeerDiscovery {

	import PeerDiscovery._

	private val _peers: mutable.Set[PeerInfo] = JavaConversions.asScalaSet(java.util.Collections.synchronizedSet(new java.util.HashSet[PeerInfo]))
	def peers: Set[PeerInfo] = this._peers.toSet

	private var monitor: PeerMonitorTask = null

	private var threadFactory: ThreadFactory = null
	private var executorPool: ThreadPoolExecutor = null
	private var rejectionHandler: RejectedExecutionHandler = null

	private val isStartedRef = new AtomicBoolean(false)
	def isStarted: Boolean = this.isStartedRef.get

	def start(): Unit = {
		this.rejectionHandler = new RejectionLogger
		this.threadFactory = new CountingThreadFactory("peer-discovery-worker")
		this.executorPool = new ThreadPoolExecutor(
			SystemProperties.CONFIG.peerDiscoveryWorkers, SystemProperties.CONFIG.peerDiscoveryWorkers, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue[Runnable](1000), this.threadFactory, this.rejectionHandler
		)
		this.monitor = new PeerMonitorTask(this.executorPool, 1, this)
		val monitorExecutor = Executors.newSingleThreadExecutor(new CountingThreadFactory("peer-discovery-monitor"))
		monitorExecutor.execute(this.monitor)

		val peerDataSeq = parsePeerDiscoveryAddresses(SystemProperties.CONFIG.seedNodes)
		addPeers(peerDataSeq)

		this.peers.foreach(each => startWorker(each))
		this.isStartedRef.set(true)
	}

	private def startWorker(peerInfo: PeerInfo): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<PeerDiscovery> Adding a new peer for discovery: %s.".format(peerInfo))
		}
		val workerTask = new WorkerTask
		workerTask.init(peerInfo, this.executorPool)
		this.executorPool.execute(workerTask)
	}

	def stop(): Unit = {
		this.executorPool.shutdown()
		this.monitor.shutdown()
		this.isStartedRef.set(false)
	}

	def addPeers(newPeers: Iterable[PeerInfo]): Unit = {
		this._peers.synchronized {
			newPeers.foreach(each => this._peers.add(each))
		}
	}
	def addPeer(newPeer: PeerInfo): Unit = addPeers(Option(newPeer))

	def addPeers(newPeers: Set[Peer]): Unit = {
		this._peers.synchronized {
			for (newPeer <- newPeers) {
				val peerInfo = new PeerInfo(new InetSocketAddress(newPeer.address, newPeer.port), newPeer.nodeId)
				if (this.isStarted && !this._peers.contains(peerInfo)) {
					startWorker(peerInfo)
				}
				this._peers.add(peerInfo)
			}
		}
	}

	private def parsePeerDiscoveryAddresses(addresses: Seq[String]): Seq[PeerInfo] = {
		addresses.flatMap {
			each => {
				try {
					val trimmed = each.trim
					val uri = URI.create(trimmed)

					Option(new PeerInfo(new InetSocketAddress(InetAddress.getByName(uri.getHost), uri.getPort), ImmutableBytes.parseHexString(uri.getUserInfo)))
				} catch {
					case e: UnknownHostException =>
						ErrorLogger.logger.warn("<PeerDiscovery> Unknown host.", e)
						logger.warn("<PeerDiscovery> Unknown host.", e)
						None
				}
			}
		}
	}

}

object PeerDiscovery {
	private val logger = LoggerFactory.getLogger("discovery")
}