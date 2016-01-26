package org.lipicalabs.lipica.core.net.peer_discovery.udp

import java.util

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.socket.DatagramPacket
import io.netty.handler.codec.MessageToMessageDecoder
import org.lipicalabs.lipica.core.net.peer_discovery.discover.DiscoveryEvent
import org.lipicalabs.lipica.core.net.peer_discovery.message.AbstractPeerDiscoveryMessage
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * UDPListener で netty のハンドラパイプラインに登録されて、
 * ネットワークと NodeManager との橋渡し役を務めるクラスです。
 *
 * 受信したUDPデータグラムをメッセージに変換して netty のパイプラインに渡します。
 * （つまり、MessageEncoder の逆方向の流れを担当します。）
 *
 * Created by IntelliJ IDEA.
 * 2015/12/22 15:47
 * YANAGISAWA, Kentaro
 */
class PacketDecoder extends MessageToMessageDecoder[DatagramPacket] {

	import PacketDecoder._

	override def decode(ctx: ChannelHandlerContext, packet: DatagramPacket, out: util.List[AnyRef]): Unit = {
		val buf = packet.content
		val encoded = new Array[Byte](buf.readableBytes)
		buf.readBytes(encoded)
		val decodedOrError: Either[Throwable, AbstractPeerDiscoveryMessage] = AbstractPeerDiscoveryMessage.decode(encoded)
		decodedOrError match {
			case Right(message) =>
				val event = new DiscoveryEvent(message, packet.sender)
				out.add(event)
			case Left(e) =>
				//UDPは各データグラムが独立しているので、
				//１個の解析に失敗したことは後続に影響を及ぼさない。
				//他ノードから送られたメッセージの解析に失敗したことを、WARNレベルにするか否かは非常に微妙だ。
				ErrorLogger.logger.warn("<PacketDecoder> Exception caught: %s".format(e.getClass.getSimpleName), e)
				logger.warn("<PacketDecoder> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}
}

object PacketDecoder {
	private val logger = LoggerFactory.getLogger("discovery")
}
