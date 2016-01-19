package org.lipicalabs.lipica.core.net.channel

import java.util.concurrent.atomic.AtomicReference

import io.netty.channel._
import io.netty.channel.socket.nio.NioSocketChannel
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.{ErrorLogger, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 * Nettyの伝送路と、このシステムのチャネルとを結びつけて
 * ChannelManagerに登録するための装置です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/13 15:47
 * YANAGISAWA, Kentaro
 */
class LipicaChannelInitializer(val nodeId: NodeId) extends ChannelInitializer[NioSocketChannel] {

	import LipicaChannelInitializer._

	private def worldManager: ComponentsMotherboard = ComponentsMotherboard.instance
	private def channelManager: ChannelManager = worldManager.channelManager


	private var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	private val initializedCallbackRef = new AtomicReference[(Channel) => _](null)
	def setInitializedCallback(f: (Channel) => _): Unit = {
		this.initializedCallbackRef.set(f)
	}

	override def initChannel(ch: NioSocketChannel): Unit = {
		try {
			logger.info("<ChannelInitializer> Opening connection: %s".format(ch))
			val channel = new Channel
			channel.init(ch.pipeline, this.nodeId, this.peerDiscoveryMode)
			if (!peerDiscoveryMode) {
				this.channelManager.add(channel)
			}
			ch.config.setRecvByteBufAllocator(new FixedRecvByteBufAllocator(16777216))
			ch.config.setOption(ChannelOption.SO_RCVBUF, Integer.valueOf(16777216))
			ch.config.setOption(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))

			ch.closeFuture.addListener(new ChannelFutureListener {
				override def operationComplete(f: ChannelFuture) = {
					if (!peerDiscoveryMode) {
						channelManager.notifyDisconnect(channel)
					}
				}
			})

			Option(this.initializedCallbackRef.get).foreach {
				proc => proc.apply(channel)
			}
		} catch {
			case e: Exception =>
				ErrorLogger.logger.warn("<ChannelInitializer> Exception caught: %s".format(e.getClass.getSimpleName), e)
				logger.warn("<ChannelInitializer> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}

}

object LipicaChannelInitializer {
	private val logger = LoggerFactory.getLogger("net")
}