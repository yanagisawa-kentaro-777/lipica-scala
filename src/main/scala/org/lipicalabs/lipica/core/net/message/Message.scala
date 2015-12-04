package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.utils.ImmutableBytes

trait Command {
	//
}

trait Message {
	def isParsed: Boolean
	def toEncodedBytes: ImmutableBytes
	def code: Byte
	def command: Command
}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:02
 * YANAGISAWA, Kentaro
 */
trait ParsedMessage extends Message {
	override val isParsed: Boolean = true
}

abstract class EncodedMessage(private val encodedBytes: ImmutableBytes) extends Message {
	override val isParsed: Boolean = false
	override val toEncodedBytes = this.encodedBytes
}
