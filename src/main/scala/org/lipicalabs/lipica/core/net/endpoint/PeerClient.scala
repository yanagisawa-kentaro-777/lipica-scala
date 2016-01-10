package org.lipicalabs.lipica.core.net.endpoint

import java.net.InetSocketAddress

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{DefaultMessageSizeEstimator, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.facade.manager.WorldManager
import org.lipicalabs.lipica.core.net.channel.LipicaChannelInitializer
import org.lipicalabs.lipica.core.utils.{ErrorLogger, CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 * クライアントとしてTCP接続を確立するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/13 15:42
 * YANAGISAWA, Kentaro
 */
class PeerClient {
	import PeerClient._

	private def worldManager: WorldManager = WorldManager.instance

	/**
	 * 他ノードに対して接続確立を試行します。
	 *
	 * @param address 接続先アドレス。
	 * @param nodeId 接続先ノードID。
	 */
	def connect(address: InetSocketAddress, nodeId: ImmutableBytes): Unit = connect(address, nodeId, discoveryMode = false)

	/**
	 * 他ノードに対して接続確立を試行します。
	 */
	def connect(address: InetSocketAddress, nodeId: ImmutableBytes, discoveryMode: Boolean): Unit = {
		this.worldManager.listener.trace("<PeerClient> Connecting to %s".format(address))
		val channelInitializer = new LipicaChannelInitializer(nodeId)
		channelInitializer.peerDiscoveryMode = discoveryMode

		try {
			val b = (new Bootstrap).group(workerGroup).channel(classOf[NioSocketChannel]).
				option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
				option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT).
				option(ChannelOption.CONNECT_TIMEOUT_MILLIS, java.lang.Integer.valueOf(SystemProperties.CONFIG.connectionTimeoutMillis)).
				remoteAddress(address).
				handler(channelInitializer)
			//クライアントとして接続する。
			val future = b.connect().sync()
			logger.debug("<PeerClient> Connection is established to %s %s.".format(nodeId.toShortString, address))
			//接続がクローズされるまで待つ。
			future.channel().closeFuture().sync()
			logger.debug("<PeerClient> Connection is closed to %s %s.".format(nodeId.toShortString, address))
		} catch {
			case e: Throwable =>
				if (discoveryMode) {
					logger.debug("<PeerClient> Exception caught: %s connecting to %s...(%s)".format(e.getClass.getSimpleName, nodeId.toShortString, address), e)
				} else {
					ErrorLogger.logger.warn("<PeerClient> Exception caught: %s connecting to %s (%s)".format(e.getClass.getSimpleName, nodeId.toShortString, address), e)
					logger.warn("<PeerClient> Exception caught: %s connecting to %s (%s)".format(e.getClass.getSimpleName, nodeId.toShortString, address), e)
				}
		}
	}

}

object PeerClient {
	private val logger = LoggerFactory.getLogger("net")
	private val workerGroup: EventLoopGroup = new NioEventLoopGroup(0, new CountingThreadFactory("peer-client-worker"))
}
