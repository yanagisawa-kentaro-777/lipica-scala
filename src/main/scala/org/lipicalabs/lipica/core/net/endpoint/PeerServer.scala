package org.lipicalabs.lipica.core.net.endpoint

import java.net.InetSocketAddress

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelOption, DefaultMessageSizeEstimator}
import io.netty.handler.logging.LoggingHandler
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.facade.manager.WorldManager
import org.lipicalabs.lipica.core.net.channel.LipicaChannelInitializer
import org.lipicalabs.lipica.core.utils.{ErrorLogger, CountingThreadFactory, ImmutableBytes}
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

	private def worldManager: WorldManager = WorldManager.instance

	private val channelInitializer: LipicaChannelInitializer = new LipicaChannelInitializer(ImmutableBytes.empty)

	def start(address: InetSocketAddress): Unit = {
		val factory = new CountingThreadFactory("peer-server")
		val bossGroup = new NioEventLoopGroup(1, factory)
		val workerGroup = new NioEventLoopGroup

		this.worldManager.listener.trace("Listening on %s".format(address))
		try {
			val b = new ServerBootstrap
			b.group(bossGroup, workerGroup)
			b.channel(classOf[NioServerSocketChannel])

			b.option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.valueOf(true))
			b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(SystemProperties.CONFIG.connectionTimeoutMillis))

			b.handler(new LoggingHandler)
			b.childHandler(this.channelInitializer)

			logger.info("<PeerServer> [%s] Listening for incoming connections on %s".format(SystemProperties.CONFIG.nodeId, address))
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