package org.lipicalabs.lipica.core.concurrent

import java.util.concurrent._
import java.util.concurrent.atomic.AtomicReference

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * 各種の処理を実行するためのスケジューラやスレッドプールのインスタンスを
 * 集中的に管理するためのクラスです。
 *
 * このような集中管理は、特にノード全体のシャットダウンの実現のために有用です。
 *
 * Created by IntelliJ IDEA.
 * @author YANAGISAWA, Kentaro
 */
class ExecutorPool private() {

	val blockQueueOpener: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("block-queue-opener"))
	val hashStoreOpener: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("hash-store-opener"))

	val syncQueue: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("sync-queue"))

	val syncManagerProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("sync-manager"))
	val syncLogger: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("sync-logger"))
	val syncManagerStarter: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("sync-manager-starter"))
	val peersPoolProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("peers-pool"))

	val listenerProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("listener-processor"))
	val peerDiscoveryMonitor: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("peer-discovery-monitor"))
	val discoverer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("discoverer"))
	val refresher: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("refresher"))
	val reconnectTimer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("reconnect-timer"))

	val channelManagerProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("channel-manager"))
	val messageQueueProcessor: ScheduledExecutorService = Executors.newScheduledThreadPool(8, new CountingThreadFactory("message-queue-timer"))
	val p2pHandlerProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("p2p-ping-timer"))
	val pongProcessor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("pong-timer"))

	val udpStarter: ExecutorService = Executors.newSingleThreadExecutor(new CountingThreadFactory("udp-starter"))

	private val peerConnectionExaminerRef = new AtomicReference[ExecutorService](null)
	def peerConnectionExaminer(numThreads: Int, queue: BlockingQueue[Runnable]): ExecutorService = {
		this.synchronized {
			val existent = peerConnectionExaminerRef.get
			if (existent eq null) {
				this.peerConnectionExaminerRef.set(
					new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.SECONDS, queue, new CountingThreadFactory("conn-examiner"))
				)
			}
			this.peerConnectionExaminerRef.get
		}
	}
	val peerDiscoveryWorker: ThreadPoolExecutor = {
		val threadFactory = new CountingThreadFactory("peer-discovery-worker")
		new ThreadPoolExecutor(
			NodeProperties.CONFIG.peerDiscoveryWorkers, NodeProperties.CONFIG.peerDiscoveryWorkers, 10, TimeUnit.SECONDS,
			new ArrayBlockingQueue[Runnable](1000), threadFactory, new RejectionLogger
		)
	}
	val serverBossGroup: EventLoopGroup = new NioEventLoopGroup(1, new CountingThreadFactory("peer-server"))
	val serverWorkerGroup: EventLoopGroup = new NioEventLoopGroup
	val clientGroup: EventLoopGroup = new NioEventLoopGroup(0, new CountingThreadFactory("peer-client-worker"))
	val discoveryGroup: EventLoopGroup = new NioEventLoopGroup
	val udpGroup: EventLoopGroup = new NioEventLoopGroup(1, new CountingThreadFactory("udp-listener"))

	val clientConnector: ExecutorService = Executors.newCachedThreadPool(new CountingThreadFactory("client-connector"))
	val txExecutor: ExecutorService = Executors.newFixedThreadPool(1, new CountingThreadFactory("tx-executor"))

	/**
	 * このオブジェクトにおいてプールされている ExecutorService を、
	 * すべてシャットダウンします。
	 */
	def shutdown(): Unit = {
		val seq = Seq(
				this.txExecutor,
				this.clientConnector,
				this.udpGroup,
				this.discoveryGroup,
				this.clientGroup,
				this.serverWorkerGroup,
				this.serverBossGroup,
				this.peerDiscoveryWorker,
				this.peerConnectionExaminerRef.get,
				this.udpStarter,
				this.pongProcessor,
				this.p2pHandlerProcessor,
				this.messageQueueProcessor,
				this.channelManagerProcessor,
				this.reconnectTimer,
				this.refresher,
				this.discoverer,
				this.peerDiscoveryMonitor,
				this.listenerProcessor,
				this.peersPoolProcessor,
				this.syncManagerStarter,
				this.syncLogger,
				this.syncManagerProcessor,
				this.syncQueue,
				this.hashStoreOpener,
				this.blockQueueOpener
			)
		shutdown(seq)
		var count = 0
		while (!isShutdown(seq) && (count < 3)) {
			count += 1
			Thread.sleep(1000L)
		}
	}

	private def isShutdown(seq: Seq[AnyRef]): Boolean = {
		var result = true
		seq.foreach {
			each => {
				each match {
					case ex: ExecutorService =>
						val ok = ex.isTerminated
						if (!ok) {
							shutdown(ex, now = true)
						}
						result &= ok
					case any => ()
				}
			}
		}
		result
	}

	private def shutdown(seq: Seq[AnyRef]): Unit = {
		for (each <- seq) {
			each match {
				case null => ()
				case group: EventLoopGroup =>
					shutdown(group)
				case ex: ExecutorService =>
					shutdown(ex, now = false)
				case any =>
					()
			}
		}
	}

	private def shutdown(ex: ExecutorService, now: Boolean): Unit = {
		if (ex ne null) {
			if (now) {
				ex.shutdownNow()
			} else {
				ex.shutdown()
			}
		}
	}

	private def shutdown(group: EventLoopGroup): Unit = {
		if (group ne null) {
			group.shutdownGracefully()
		}
	}

}

object ExecutorPool {
	val instance = new ExecutorPool
}

class RejectionLogger extends RejectedExecutionHandler {
	override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
		ErrorLogger.logger.warn("%s is rejected.".format(r))
		RejectionLogger.logger.warn("%s is rejected.".format(r))
	}
}

object RejectionLogger {
	private val logger = LoggerFactory.getLogger("wire")
}