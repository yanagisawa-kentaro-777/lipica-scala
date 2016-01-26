package org.lipicalabs.lipica.core.net.shh

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.lipicalabs.lipica.core.net.channel.MessageQueue

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:36
 * YANAGISAWA, Kentaro
 */
class ShhHandler extends SimpleChannelInboundHandler[ShhMessage] {

	private var _messageQueue: MessageQueue = null
	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v

	private var _privateKey: ECKeyPair = null
	def privateKey_=(v: ECKeyPair): Unit = this._privateKey = v
	def privateKey: ECKeyPair = this._privateKey

	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: ShhMessage): Unit = {
		//TODO 未実装。
	}

	def activate(): Unit = {
		//TODO
	}

}

object ShhHandler {
	val Version: Byte = 2
}
