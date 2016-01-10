package org.lipicalabs.lipica.core.net.peer_discovery.udp

import java.net.InetSocketAddress

import io.netty.buffer.Unpooled
import io.netty.channel.socket.DatagramPacket
import io.netty.channel.socket.nio.NioDatagramChannel
import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.net.peer_discovery.NodeManager
import org.lipicalabs.lipica.core.net.peer_discovery.discover.DiscoveryEvent
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * UDPListener で netty のハンドラパイプラインに登録されて、
 * ネットワークと NodeManager との橋渡し役を務めるクラスです。
 *
 * 送信すべきメッセージをUDPデータグラムに変換して netty に渡します。
 * （つまり、PacketDecoder の逆側です。）
 *
 * Created by IntelliJ IDEA.
 * 2015/12/21 10:54
 * YANAGISAWA, Kentaro
 */
class MessageHandler(private val channel: NioDatagramChannel, private val nodeManager: NodeManager)  extends SimpleChannelInboundHandler[DiscoveryEvent] {
	import MessageHandler._

	override def channelActive(ctx: ChannelHandlerContext): Unit = {
		this.nodeManager.channelActivated()
	}

	override def channelRead0(ctx: ChannelHandlerContext, event: DiscoveryEvent): Unit = {
		this.nodeManager.handleInbound(event)
	}

	def accept(event: DiscoveryEvent): Unit = {
		val address = event.address
		//println("Sending: %s to %s".format(event.message.messageType, address))
		sendPacket(event.message.packet, address)
	}

	private def sendPacket(wire: Array[Byte], address: InetSocketAddress): Unit = {
		val packet = new DatagramPacket(Unpooled.copiedBuffer(wire), address)
		this.channel.write(packet)
		this.channel.flush()
	}

	override def channelReadComplete(ctx: ChannelHandlerContext): Unit = ctx.flush()

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		ErrorLogger.logger.warn("<MessageHandler> Exception caught: %s".format(cause.getClass.getSimpleName), cause)
		logger.warn("<MessageHandler> Exception caught: %s".format(cause.getClass.getSimpleName), cause)
		ctx.close()
	}

}

object MessageHandler {
	private val logger = LoggerFactory.getLogger("discover")
}