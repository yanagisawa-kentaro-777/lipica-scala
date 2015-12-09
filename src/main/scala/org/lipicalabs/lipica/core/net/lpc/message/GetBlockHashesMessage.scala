package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 20:57
 * YANAGISAWA, Kentaro
 */
class GetBlockHashesMessage(private val hash: ImmutableBytes, private val maxBlocks: Int) extends LpcMessage {

	override def toEncodedBytes = {
		val encodedHash = RBACCodec.Encoder.encode(this.hash)
		val encodedMaxBlocks = RBACCodec.Encoder.encode(this.maxBlocks)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHash, encodedMaxBlocks))
	}

	override def code = LpcMessageCode.GetBlockHashes.asByte

}

object GetBlockHashesMessage {
	def decode(encodedBytes: ImmutableBytes): GetBlockHashesMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new GetBlockHashesMessage(items.head.bytes, items(1).asInt)
	}
}
