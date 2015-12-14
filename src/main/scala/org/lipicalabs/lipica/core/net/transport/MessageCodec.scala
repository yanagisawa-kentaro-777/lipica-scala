package org.lipicalabs.lipica.core.net.transport

import java.util

import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.handler.codec.ByteToMessageCodec
import org.lipicalabs.lipica.core.net.message.Message

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:11
 * YANAGISAWA, Kentaro
 */
class MessageCodec extends ByteToMessageCodec[Message] {
	//TODO 未実装。

	val initiator = new InitiateHandler

	class InitiateHandler extends ChannelInboundHandlerAdapter {
		override def channelActive(ctx: ChannelHandlerContext): Unit = {
			//TODO 未実装。
		}
	}

	override def encode(channelHandlerContext: ChannelHandlerContext, i: Message, byteBuf: ByteBuf) = ???
	override def decode(channelHandlerContext: ChannelHandlerContext, byteBuf: ByteBuf, list: util.List[AnyRef]) = ???
}

