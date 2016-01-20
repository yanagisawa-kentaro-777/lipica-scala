package org.lipicalabs.lipica.core.concurrent

import java.util.concurrent.{ScheduledExecutorService, ExecutorService, Executors}

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup

/**
 * Created by IntelliJ IDEA.
 * 2016/01/20 11:00
 * YANAGISAWA, Kentaro
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

	val serverBossGroup: EventLoopGroup = new NioEventLoopGroup(1, new CountingThreadFactory("peer-server"))
	val serverWorkerGroup: EventLoopGroup = new NioEventLoopGroup
	val clientGroup: EventLoopGroup = new NioEventLoopGroup(0, new CountingThreadFactory("peer-client-worker"))
	val discoveryGroup: EventLoopGroup = new NioEventLoopGroup
	val udpGroup: EventLoopGroup = new NioEventLoopGroup(1, new CountingThreadFactory("udp-listener"))

}

object ExecutorPool {
	val instance = new ExecutorPool
}