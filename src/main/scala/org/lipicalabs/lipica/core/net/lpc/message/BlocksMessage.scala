package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 20:42
 * YANAGISAWA, Kentaro
 */
case class BlocksMessage(blocks: Seq[Block]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blocks.map(each => each.encode)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.Blocks.asByte

	override def toString: String = "BlocksMessage(%,d blocks)".format(this.blocks.size)

}

object BlocksMessage {
	def decode(encodedBytes: ImmutableBytes): BlocksMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val blocks = items.map(each => Block.decode(each.items))
		new BlocksMessage(blocks)
	}
}
