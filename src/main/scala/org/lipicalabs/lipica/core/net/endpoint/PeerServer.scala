package org.lipicalabs.lipica.core.net.endpoint

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelOption, DefaultMessageSizeEstimator}
import io.netty.handler.logging.LoggingHandler
import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.channel.LipicaChannelInitializer
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * 他ノードからのTCP接続を待ち受けるモジュールです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/23 13:52
 * YANAGISAWA, Kentaro
 */
class PeerServer {
	import PeerServer._

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private val channelInitializer: LipicaChannelInitializer = new LipicaChannelInitializer(NodeId.empty)

	def start(address: InetSocketAddress): Unit = {
		val bossGroup = ExecutorPool.instance.serverBossGroup
		val workerGroup = ExecutorPool.instance.serverWorkerGroup

		this.componentsMotherboard.listener.trace("Listening on %s".format(address))
		try {
			val b = new ServerBootstrap
			b.group(bossGroup, workerGroup)
			b.channel(classOf[NioServerSocketChannel])

			b.option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.valueOf(true))
			b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(NodeProperties.CONFIG.connectionTimeoutMillis))

			b.handler(new LoggingHandler)
			b.childHandler(this.channelInitializer)

			logger.info("<PeerServer> [%s] Listening for incoming connections on %s".format(NodeProperties.CONFIG.nodeId, address))
			val f = b.bind(address).sync()
			f.channel.closeFuture.sync()

			if (logger.isDebugEnabled) {
				logger.debug("<PeerServer> Connection is closed.")
			}
		} catch {
			case e: Exception =>
				ErrorLogger.logger.warn("<PeerServer> Exception caught: %s".format(e.getClass.getSimpleName), e)
				logger.warn("<PeerServer> Exception caught: %s".format(e.getClass.getSimpleName), e)
		} finally {
			workerGroup.shutdownGracefully()
		}
	}

}

object PeerServer {
	private val logger = LoggerFactory.getLogger("net")
}