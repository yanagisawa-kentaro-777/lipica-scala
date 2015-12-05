package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.net.p2p.PingMessage
import org.lipicalabs.lipica.core.net.p2p.PongMessage
import org.lipicalabs.lipica.core.net.p2p.GetPeersMessage
import org.lipicalabs.lipica.core.net.p2p.DisconnectMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:27
 * YANAGISAWA, Kentaro
 */
object ImmutableMessages {

	val PingMessage: PingMessage = new PingMessage
	val PongMessage: PongMessage = new PongMessage
	val GetPeersMessage: GetPeersMessage = new GetPeersMessage
	val DisconnectMessage: DisconnectMessage = new DisconnectMessage(ReasonCode.Requested)

	//TODO 未実装。
}
