package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.Pong
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 12:53
 * YANAGISAWA, Kentaro
 */
case class PongMessage() extends P2PMessage {
	import PongMessage._

	override def toEncodedBytes = Payload
	override def code = Pong.asByte
}

object PongMessage {
	private val Payload = ImmutableBytes.parseHexString("C0")
}