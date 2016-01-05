package org.lipicalabs.lipica.core.net.client

import java.net.InetAddress
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

import io.netty.bootstrap.Bootstrap
import io.netty.channel.{DefaultMessageSizeEstimator, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.channel.LipicaChannelInitializer
import org.lipicalabs.lipica.core.utils.ImmutableBytes
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
	 *
	 * @param address 接続先アドレス。
	 * @param nodeId 接続先ノードID。
	 */
	def connect(address: InetAddress, port: Int, nodeId: ImmutableBytes): Unit = connect(address, port, nodeId, discoveryMode = false)

	def connect(address: InetAddress, port: Int, nodeId: ImmutableBytes, discoveryMode: Boolean): Unit = {
		this.worldManager.listener.trace("<PeerClient> Connecting to [%s]:%d".format(address, port))
		val channelInitializer = new LipicaChannelInitializer(nodeId)
		channelInitializer.peerDiscoveryMode = discoveryMode

		try {
			val b = (new Bootstrap).group(workerGroup).channel(classOf[NioSocketChannel]).
				option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
				option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT).
				option(ChannelOption.CONNECT_TIMEOUT_MILLIS, java.lang.Integer.valueOf(SystemProperties.CONFIG.connectionTimeoutMillis)).
				remoteAddress(address, port).
				handler(channelInitializer)
			//クライアントとして接続する。
			val future = b.connect().sync()
			logger.debug("<PeerClient> Connection is established to %s [%s]:%d.".format(nodeId.toShortString, address, port))
			//接続がクローズされるまで待つ。
			future.channel().closeFuture().sync()
			logger.debug("<PeerClient> Connection is closed to %s [%s]:%d.".format(nodeId.toShortString, address, port))
		} catch {
			case e: Throwable =>
				if (discoveryMode) {
					logger.debug("<PeerClient> Exception caught: %s connecting to %s...([%s]:%d)".format(e.getClass.getSimpleName, nodeId.toShortString, address, port), e)
				} else {
					logger.warn("<PeerClient> Exception caught: %s connecting to %s...([%s]:%d)".format(e.getClass.getSimpleName, nodeId.toShortString, address, port), e)
				}
		}
	}

}

object PeerClient {
	private val logger = LoggerFactory.getLogger("net")

	private val workerGroup: EventLoopGroup = new NioEventLoopGroup(
		0,
		new ThreadFactory {
			private val count = new AtomicInteger(0)
			override def newThread(r: Runnable): Thread = {
				new Thread(r, "LpcClientWorker-" + this.count.getAndIncrement)
			}
		}
	)
}
