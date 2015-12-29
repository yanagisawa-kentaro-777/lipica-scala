package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetAddress

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel._
import io.netty.channel.socket.nio.NioSocketChannel
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.channel.Channel
import org.lipicalabs.lipica.core.net.lpc.handler.{Lpc0, LpcHandler}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.p2p.{HelloMessage, P2PHandler}
import org.lipicalabs.lipica.core.net.channel.LipicaChannelInitializer
import org.lipicalabs.lipica.core.net.shh.ShhHandler
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzHandler
import org.lipicalabs.lipica.core.net.transport.MessageCodec
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 19:49
 * YANAGISAWA, Kentaro
 */
class DiscoveryChannel {
	import DiscoveryChannel._

	private var _peerDiscoveryMode: Boolean = false

	private def worldManager: WorldManager = WorldManager.instance

	private val messageQueue: MessageQueue = new MessageQueue
	private val p2pHandler: P2PHandler = new P2PHandler(this.messageQueue)
	private val lpcHandler: LpcHandler = new Lpc0
	private val shhHandler: ShhHandler = new ShhHandler
	private val bzzHandler: BzzHandler = new BzzHandler

	def getHelloHandshake: HelloMessage = this.p2pHandler.handshakeHelloMessage
	def getStatusHandshake: StatusMessage = this.lpcHandler.getHandshakeStatusMessage

	def connect(host: InetAddress, port: Int, remoteId: String): Unit = {
		val workerGroup: EventLoopGroup = new NioEventLoopGroup
		this.worldManager.listener.trace("<DiscoveryChannel> Connecting to [%s]:%d".format(host, port))
		try {
			val b = (new Bootstrap).group(workerGroup).channel(classOf[NioSocketChannel]).
				option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
				option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT).
				option(ChannelOption.CONNECT_TIMEOUT_MILLIS, java.lang.Integer.valueOf(SystemProperties.CONFIG.connectionTimeoutMillis)).
				remoteAddress(host, port)

			this.p2pHandler.messageQueue = this.messageQueue
			this.p2pHandler.peerDiscoveryMode = true

			this.lpcHandler.messageQueue = this.messageQueue
			this.lpcHandler.peerDiscoveryMode = true

			this.shhHandler.messageQueue = this.messageQueue
			this.bzzHandler.messageQueue = this.messageQueue

			//val codec = new MessageCodec

			val initializer = new LipicaChannelInitializer(remoteId)
			initializer.peerDiscoveryMode = true
			initializer.setInitializedCallback((channel: Channel) => {
				this.p2pHandler.channel = channel
				this.lpcHandler.channel = channel
			})
			b.handler(initializer)

//			b.handler(
//				new ChannelInitializer[NioSocketChannel] {
//					override def initChannel(ch: NioSocketChannel): Unit = {
//						ch.pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(SystemProperties.CONFIG.peerChannelReadTimeoutSeconds, TimeUnit.SECONDS))
//						ch.pipeline.addLast("initiator", codec.initiator)
//						ch.pipeline.addLast("messageCodec", codec)
//						ch.pipeline.addLast(Capability.P2P, p2pHandler)
//						ch.pipeline.addLast(Capability.LPC, lpcHandler)
//						ch.pipeline.addLast(Capability.SHH, shhHandler)
//						ch.pipeline.addLast(Capability.BZZ, bzzHandler)
//
//						ch.config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(32368))
//						ch.config.setOption(ChannelOption.SO_RCVBUF, Integer.valueOf(32368))
//						ch.config.setOption(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
//					}
//				}
//			)
			//クライアントとして接続する。
			val future = b.connect().sync()
			logger.debug("<DiscoveryChannel> Connection is established to [%s]:%d.".format(host, port))
			//接続がクローズされるまで待つ。
			future.channel().closeFuture().sync()
			logger.debug("<DiscoveryChannel> Connection is closed to [%s]:%d.".format(host, port))
		} catch {
			case e: Throwable =>
				logger.debug("<PeerClient> Exception caught: %s".format(e.getClass.getSimpleName), e)
		} finally {
			workerGroup.shutdownGracefully()
		}
	}

}

object DiscoveryChannel {
	private val logger = LoggerFactory.getLogger("net")
}