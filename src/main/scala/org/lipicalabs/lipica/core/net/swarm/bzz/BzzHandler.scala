package org.lipicalabs.lipica.core.net.swarm.bzz

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:37
 * YANAGISAWA, Kentaro
 */
class BzzHandler extends SimpleChannelInboundHandler[BzzMessage] {

	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: BzzMessage): Unit = {
		//TODO 未実装。
	}

}


object BzzHandler {
	val Version: Byte = 0
}
