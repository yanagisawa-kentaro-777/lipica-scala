package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Ping
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 12:45
 * YANAGISAWA, Kentaro
 */
class PingMessage extends P2PParsedMessage {

	import PingMessage._

	override def toEncodedBytes = Payload
	override def answerMessage: Option[Class[_ <: P2PParsedMessage]] = Option(PingMessage.answerMessage)
	override def code = Ping.asByte
}

object PingMessage {
	private val Payload = ImmutableBytes.parseHexString("77")
	private val answerMessage: Class[_ <: P2PParsedMessage] = (new PongMessage).getClass
}