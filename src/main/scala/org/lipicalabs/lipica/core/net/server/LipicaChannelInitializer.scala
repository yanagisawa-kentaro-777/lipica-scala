package org.lipicalabs.lipica.core.net.server

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.nio.NioSocketChannel

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 15:47
 * YANAGISAWA, Kentaro
 */
class LipicaChannelInitializer(val remoteId: String) extends ChannelInitializer[NioSocketChannel] {
	//TODO 未実装。
	override def initChannel(c: NioSocketChannel) = ???

	def peerDiscoveryMode: Boolean = ???
	def peerDiscoveryMode_=(v: Boolean): Unit = ???
}
