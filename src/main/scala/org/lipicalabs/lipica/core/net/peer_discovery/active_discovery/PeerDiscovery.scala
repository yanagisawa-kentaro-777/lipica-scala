package org.lipicalabs.lipica.core.net.peer_discovery.active_discovery

import java.net.{InetSocketAddress, InetAddress, URI, UnknownHostException}
import java.util.concurrent._
import java.util.concurrent.atomic.AtomicBoolean

import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, PeerInfo}
import org.lipicalabs.lipica.core.utils.ErrorLogger
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

	private val workerExecutor: ThreadPoolExecutor = ExecutorPool.instance.peerDiscoveryWorker

	private val isStartedRef = new AtomicBoolean(false)
	def isStarted: Boolean = this.isStartedRef.get

	def start(): Unit = {
		this.monitor = new PeerMonitorTask(this.workerExecutor, 1, this)
		val monitorExecutor = ExecutorPool.instance.peerDiscoveryMonitor
		monitorExecutor.execute(this.monitor)

		val peerDataSeq = parsePeerDiscoveryAddresses(NodeProperties.instance.seedNodes)
		addPeers(peerDataSeq)

		this.peers.foreach(each => startWorker(each))
		this.isStartedRef.set(true)
	}

	private def startWorker(peerInfo: PeerInfo): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<PeerDiscovery> Adding a new peer for discovery: %s.".format(peerInfo))
		}
		val workerTask = new WorkerTask
		workerTask.init(peerInfo, this.workerExecutor)
		this.workerExecutor.execute(workerTask)
	}

	def stop(): Unit = {
		this.workerExecutor.shutdown()
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

	private def parsePeerDiscoveryAddresses(addresses: Seq[URI]): Seq[PeerInfo] = {
		addresses.flatMap {
			eachUri => {
				try {
					Option(new PeerInfo(new InetSocketAddress(InetAddress.getByName(eachUri.getHost), eachUri.getPort), NodeId.parseHexString(eachUri.getUserInfo)))
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