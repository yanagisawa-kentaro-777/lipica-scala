package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.{UnknownHostException, InetAddress}
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent._

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.net.p2p.Peer
import org.slf4j.LoggerFactory

import scala.collection.{mutable, JavaConversions}

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
		this.threadFactory = Executors.defaultThreadFactory
		this.executorPool = new ThreadPoolExecutor(
			SystemProperties.CONFIG.peerDiscoveryWorkers, SystemProperties.CONFIG.peerDiscoveryWorkers, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue[Runnable](1000), this.threadFactory, this.rejectionHandler
		)
		this.monitor = new PeerMonitorTask(this.executorPool, 1, this)
		val monitorThread = new Thread(this.monitor)
		monitorThread.start()

		val peerDataSeq = parsePeerDiscoveryAddresses(SystemProperties.CONFIG.peerDiscoveryAddresses)
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
				val peerInfo = new PeerInfo(newPeer.address, newPeer.port, newPeer.peerId)
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
					val idx = trimmed.lastIndexOf(':')
					val addressPart = trimmed.substring(0, idx).trim
					val portPart = trimmed.substring(idx + 1, trimmed.length)
					val address = InetAddress.getByName(addressPart)
					val port = portPart.toInt

					Option(new PeerInfo(address, port, ""))
				} catch {
					case e: UnknownHostException =>
						logger.warn("<PeerDiscovery> Unknown host.", e)
						None
				}
			}
		}
	}

}

object PeerDiscovery {
	private val logger = LoggerFactory.getLogger("peerdiscovery")
}