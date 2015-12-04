package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.{EncodedMessage, ParsedMessage}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:50
 * YANAGISAWA, Kentaro
 */
abstract class P2PParsedMessage extends ParsedMessage {
	override def command = P2PMessageCode.fromByte(this.code)
}

abstract class P2PEncodedMessage(_bytes: ImmutableBytes) extends EncodedMessage(_bytes) {
	override def command = P2PMessageCode.fromByte(this.code)
}