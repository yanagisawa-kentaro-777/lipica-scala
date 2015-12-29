package org.lipicalabs.lipica.core.net.transport.discover

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.handler.codec.MessageToMessageDecoder
import org.lipicalabs.lipica.core.net.transport.TransportMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/22 15:47
 * YANAGISAWA, Kentaro
 */
class PacketDecoder extends MessageToMessageDecoder[DatagramPacket] {

	override def decode(ctx: ChannelHandlerContext, packet: DatagramPacket, out: util.List[AnyRef]): Unit = {
		println("packet decoder")//TODO
		val buf = packet.content
		val encoded = new Array[Byte](buf.readableBytes)
		buf.readBytes(encoded)
		val message = TransportMessage.decode(encoded)
		val event = new DiscoveryEvent(message, packet.sender)
		out.add(event)
	}

}
