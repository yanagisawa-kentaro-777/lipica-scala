package org.lipicalabs.lipica.core.net.shh

import org.lipicalabs.lipica.core.net.message.{Message, MessageFactory}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/20 12:53
 * YANAGISAWA, Kentaro
 */
class ShhMessageFactory extends MessageFactory {
	//TODO 未実装。
	override def create(code: Byte, encodedBytes: ImmutableBytes): Option[Message] = {
		None
	}

}
