package org.lipicalabs.lipica.core.net.swarm.bzz

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.net.channel.MessageQueue

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:37
 * YANAGISAWA, Kentaro
 */
class BzzHandler extends SimpleChannelInboundHandler[BzzMessage] {

	private var _messageQueue: MessageQueue = null

	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v
	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: BzzMessage): Unit = {
		//TODO 未実装。
	}

	def activate(): Unit = {
		//TODO
	}

}


object BzzHandler {
	val Version: Byte = 0
}
