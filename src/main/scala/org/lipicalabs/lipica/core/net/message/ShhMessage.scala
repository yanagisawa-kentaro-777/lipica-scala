package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:25
 * YANAGISAWA, Kentaro
 */
trait ShhParsedMessage extends ParsedMessage {
	//
}

abstract class ShhEncodedMessage(_bytes: ImmutableBytes) extends EncodedMessage(_bytes) {
	//
}