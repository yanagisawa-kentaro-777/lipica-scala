package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.{ImmutableMessages, MessageFactory}
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode._
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 13:38
 * YANAGISAWA, Kentaro
 */
class P2PMessageFactory extends MessageFactory {
	override def create(code: Byte, encodedBytes: ImmutableBytes) = {
		val result =
			P2PMessageCode.fromByte(code) match {
				case Hello => HelloMessage.decode(encodedBytes)
				case Disconnect => DisconnectMessage.decode(encodedBytes)
				case Ping => ImmutableMessages.PingMessage
				case Pong => ImmutableMessages.PongMessage
				case GetPeers => ImmutableMessages.GetPeersMessage
				case Peers => PeersMessage.decode(encodedBytes)
				case _ => null
			}
		Option(result)
	}
}
