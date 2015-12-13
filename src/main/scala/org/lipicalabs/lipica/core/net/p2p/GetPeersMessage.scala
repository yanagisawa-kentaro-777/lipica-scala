package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode.GetPeers
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 13:00
 * YANAGISAWA, Kentaro
 */
case class GetPeersMessage() extends P2PMessage {
	import GetPeersMessage._

	override def toEncodedBytes = FixedPayload
	override def code = GetPeers.asByte
}

object GetPeersMessage {
	private val FixedPayload = ImmutableBytes.parseHexString("C104")
}