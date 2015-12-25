package org.lipicalabs.lipica.core.net.server

import io.netty.channel._
import io.netty.channel.socket.nio.NioSocketChannel
import org.lipicalabs.lipica.core.manager.WorldManager
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 15:47
 * YANAGISAWA, Kentaro
 */
class LipicaChannelInitializer(val remoteId: String) extends ChannelInitializer[NioSocketChannel] {

	import LipicaChannelInitializer._

	private def worldManager: WorldManager = WorldManager.instance
	private def channelManager: ChannelManager = worldManager.channelManager


	private var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	override def initChannel(ch: NioSocketChannel): Unit = {
		try {
			logger.info("<ChannelInitializer> Opening connection: %s".format(ch))
			val channel = new Channel
			channel.init(ch.pipeline, this.remoteId, this.peerDiscoveryMode)
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
		} catch {
			case e: Exception =>
				logger.warn("<ChannelInitializer> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}

}

object LipicaChannelInitializer {
	private val logger = LoggerFactory.getLogger("net")
}