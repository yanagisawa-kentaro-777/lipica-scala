package org.lipicalabs.lipica.core.net.peer_discovery

import java.util.concurrent.ThreadPoolExecutor

import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:42
 * YANAGISAWA, Kentaro
 */
class WorkerTask extends Runnable {
	import WorkerTask._

	private var peerInfo: PeerInfo = null
	private var executor: ThreadPoolExecutor = null

	def init(peer: PeerInfo, pool: ThreadPoolExecutor): Unit = {
		this.peerInfo = peer
		this.executor = pool
	}

	override def run(): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<WorkerTask> Starting on %s".format(Thread.currentThread.getName))
		}
		processCommand()
		if (logger.isDebugEnabled) {
			logger.debug("<WorkerTask> Executed on %s".format(Thread.currentThread.getName))
		}

		sleep(1000L)
		this.executor.execute(this)
	}

	private def processCommand(): Unit = {
		try {
			val discoveryChannel = new DiscoveryChannel
			discoveryChannel.connect(this.peerInfo.address, this.peerInfo.port)
			peerInfo.online = true

			peerInfo.handshakeHelloMessage = discoveryChannel.getHelloHandshake
			peerInfo.statusMessage = discoveryChannel.getStatusHandshake
			logger.info("<WorkerTask> Peer is online: %s".format(this.peerInfo))
		} catch {
			case any: Throwable =>
				logger.info("<WorkerTask> Exception caught: %s".format(any.getClass.getSimpleName), any)
				this.peerInfo.online = false
		} finally {
			logger.info("<WorkerTask> Peer %s isOnline ? %s".format(this.peerInfo, this.peerInfo.online)
			this.peerInfo.lastCheckTime = System.currentTimeMillis
		}
	}

	private def sleep(millis: Long): Unit = {
		try {
			Thread.sleep(millis)
		} catch {
			case e: InterruptedException => ()
		}
	}

}

object WorkerTask {
	private val logger = LoggerFactory.getLogger("peerdiscovery")
}