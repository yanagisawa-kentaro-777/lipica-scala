package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.ParsedMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:50
 * YANAGISAWA, Kentaro
 */
abstract class P2PMessage extends ParsedMessage {
	override def command = P2PMessageCode.fromByte(this.code)
}
