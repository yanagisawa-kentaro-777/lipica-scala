package org.lipicalabs.lipica.core.net.server

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{DefaultMessageSizeEstimator, ChannelOption}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LoggingHandler
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/23 13:52
 * YANAGISAWA, Kentaro
 */
class PeerServer {
	import PeerServer._

	//TODO auto wiring
	private val channelManager: ChannelManager = ???
	private val worldManager: WorldManager = ???
	private val channelInitializer: LipicaChannelInitializer = new LipicaChannelInitializer("")

	def start(port: Int): Unit = {
		val bossGroup = new NioEventLoopGroup(1)
		val workerGroup = new NioEventLoopGroup

		this.worldManager.listener.trace("Listening on port %d".format(port))
		try {
			val b = new ServerBootstrap
			b.group(bossGroup, workerGroup)
			b.channel(classOf[NioServerSocketChannel])

			b.option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.valueOf(true))
			b.option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT)
			b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, Integer.valueOf(SystemProperties.CONFIG.peerConnectionTimeoutMillis))

			b.handler(new LoggingHandler)
			b.childHandler(this.channelInitializer)

			logger.info("<PeerServer> [%s] Listening for incoming connections: Port: %d".format(SystemProperties.CONFIG.nodeId, port))
			val f = b.bind(port).sync()
			f.channel.closeFuture.sync()

			if (logger.isDebugEnabled) {
				logger.debug("<PeerServer> Connection is closed.")
			}
		} catch {
			case e: Exception =>
				if (logger.isDebugEnabled) {
					logger.debug("<PeerServer> Exception caught: %s".format(e.getClass.getSimpleName), e)
				}
				throw new RuntimeException(e)
		} finally {
			workerGroup.shutdownGracefully()
		}
	}

}

object PeerServer {
	private val logger = LoggerFactory.getLogger("net")
}