package org.lipicalabs.lipica.core.net.shh

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:36
 * YANAGISAWA, Kentaro
 */
class ShhHandler extends SimpleChannelInboundHandler[ShhMessage] {

	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: ShhMessage): Unit = {
		//TODO 未実装。
	}

}

object ShhHandler {
	val Version: Byte = 2
}
