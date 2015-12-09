package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 20:36
 * YANAGISAWA, Kentaro
 */
class BlockHashesMessage(private val blockHashes: Seq[ImmutableBytes]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.BlockHashes.asByte

}

object BlockHashesMessage {

	def decode(encodedBytes: ImmutableBytes): BlockHashesMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new BlockHashesMessage(items.map(_.bytes))
	}

}