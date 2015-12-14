package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetAddress
import java.util.concurrent.{TimeUnit, ThreadFactory}
import java.util.concurrent.atomic.AtomicInteger

import io.netty.bootstrap.Bootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel._
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.timeout.ReadTimeoutHandler
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.handler.LpcHandler
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.p2p.{HelloMessage, P2PHandler}
import org.lipicalabs.lipica.core.net.server.LipicaChannelInitializer
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

	//TODO auto wiring
	private val worldManager: WorldManager = ???
	private val messageQueue: MessageQueue = ???
	private val p2pHandler: P2PHandler = ???
	private val lpcHandler: LpcHandler = ???
	private val shhHandler: ShhHandler = ???
	private val bzzHandler: BzzHandler = ???

	def getHelloHandshake: HelloMessage = this.p2pHandler.handshakeHelloMessage
	def getStatusHandshake: StatusMessage = this.lpcHandler.getHandshakeStatusMessage

	def connect(host: InetAddress, port: Int): Unit = {
		val workerGroup: EventLoopGroup = new NioEventLoopGroup
		this.worldManager.listener.trace("<DiscoveryChannel> Connecting to [%s]:%d".format(host, port))
		try {
			val b = (new Bootstrap).group(workerGroup).channel(classOf[NioSocketChannel]).
				option(ChannelOption.SO_KEEPALIVE, java.lang.Boolean.TRUE).
				option(ChannelOption.MESSAGE_SIZE_ESTIMATOR, DefaultMessageSizeEstimator.DEFAULT).
				option(ChannelOption.CONNECT_TIMEOUT_MILLIS, java.lang.Integer.valueOf(SystemProperties.CONFIG.peerConnectionTimeoutMillis)).
				remoteAddress(host, port)

			this.p2pHandler.messageQueue = this.messageQueue
			this.p2pHandler.peerDiscoveryMode = true

			this.lpcHandler.messageQueue = this.messageQueue
			this.lpcHandler.peerDiscoveryMode = true

			this.shhHandler.messageQueue = this.messageQueue
			this.bzzHandler.messageQueue = this.messageQueue

			val codec: MessageCodec = new MessageCodec()//TODO auto wiring

			b.handler(
				new ChannelInitializer[NioSocketChannel] {
					override def initChannel(ch: NioSocketChannel): Unit = {
						ch.pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(SystemProperties.CONFIG.peerChannelReadTimeoutSeconds, TimeUnit.SECONDS))
						ch.pipeline.addLast("initiator", codec.initiator)
						ch.pipeline.addLast("messageCodec", codec)
						ch.pipeline.addLast(Capability.P2P, p2pHandler)
						ch.pipeline.addLast(Capability.LPC, lpcHandler)
						ch.pipeline.addLast(Capability.SHH, shhHandler)
						ch.pipeline.addLast(Capability.BZZ, bzzHandler)

						ch.config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(32368))
						ch.config.setOption(ChannelOption.SO_RCVBUF, 32368)
						ch.config.setOption(ChannelOption.SO_BACKLOG, 1024)
					}
				}
			)
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