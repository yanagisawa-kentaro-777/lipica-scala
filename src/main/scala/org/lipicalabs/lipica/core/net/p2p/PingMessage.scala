package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.ImmutableMessages
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Ping
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 12:45
 * YANAGISAWA, Kentaro
 */
case class PingMessage() extends P2PMessage {
	import PingMessage._

	override def toEncodedBytes = Payload
	override def answerMessage: Option[Class[_ <: P2PMessage]] = Option(PingMessage.answerMessage)
	override def code = Ping.asByte
}

object PingMessage {
	private val Payload = ImmutableBytes.parseHexString("C0")
	private val answerMessage: Class[_ <: P2PMessage] = ImmutableMessages.PongMessage.getClass
}
