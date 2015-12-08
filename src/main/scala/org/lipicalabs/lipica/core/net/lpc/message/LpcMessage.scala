package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.ParsedMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/08 19:57
 * YANAGISAWA, Kentaro
 */
abstract class LpcMessage extends ParsedMessage {
	override def command = LpcMessageCode.fromByte(this.code)
}
