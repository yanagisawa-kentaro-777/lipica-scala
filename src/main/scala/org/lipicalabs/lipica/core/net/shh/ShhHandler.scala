package org.lipicalabs.lipica.core.net.shh

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.net.MessageQueue

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:36
 * YANAGISAWA, Kentaro
 */
class ShhHandler extends SimpleChannelInboundHandler[ShhMessage] {

	private var _messageQueue: MessageQueue = null
	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v

	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: ShhMessage): Unit = {
		//TODO 未実装。
	}

}

object ShhHandler {
	val Version: Byte = 2
}
