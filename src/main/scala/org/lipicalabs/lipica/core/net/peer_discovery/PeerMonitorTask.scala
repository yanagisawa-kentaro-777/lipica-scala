package org.lipicalabs.lipica.core.net.peer_discovery

import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:27
 * YANAGISAWA, Kentaro
 */
class PeerMonitorTask(private val executor: ThreadPoolExecutor, private val delaySeconds: Int, private val peerDiscovery: PeerDiscovery) extends Runnable {
	import PeerMonitorTask._

	private val runningRef = new AtomicBoolean(true)
	private def isRunning: Boolean = this.runningRef.get

	override def run(): Unit = {
		while (this.isRunning) {
			try {
				val log = "[PeerMonitor %,d/%,d] Active=%,d Completed=%,d Task=%,d IsShutdown=%s IsTerminated=%s PeersDiscovered=%,d".format(
					this.executor.getPoolSize, this.executor.getCorePoolSize,
					this.executor.getActiveCount, this.executor.getCompletedTaskCount, this.executor.getTaskCount, this.executor.isShutdown, this.executor.isTerminated,
					this.peerDiscovery.peers.size
				)
				logger.info(log)

				Thread.sleep(this.delaySeconds * 1000)
			} catch {
				case e: InterruptedException =>
					this.shutdown()
				case any: Throwable =>
					logger.warn("<PeerMonitorTask> Exception caught %s".format(any.getClass.getSimpleName), any)
			}
		}
	}

	def shutdown(): Unit = this.runningRef.set(true)
}

object PeerMonitorTask {
	private val logger = LoggerFactory.getLogger("peermonitor")
}
