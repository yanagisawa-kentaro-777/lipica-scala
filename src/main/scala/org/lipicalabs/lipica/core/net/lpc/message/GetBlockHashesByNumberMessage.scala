package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 20:46
 * YANAGISAWA, Kentaro
 */
class GetBlockHashesByNumberMessage(private val blockNumber: Long, private val maxBlocks: Int) extends LpcMessage {

	override def toEncodedBytes = {
		val encodedBlockNumber = RBACCodec.Encoder.encode(BigInt(this.blockNumber))
		val encodedMaxBlocks = RBACCodec.Encoder.encode(this.maxBlocks)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedBlockNumber, encodedMaxBlocks))
	}

	override def code = LpcMessageCode.GetBlockHashesByNumber.asByte

}

object GetBlockHashesByNumberMessage {
	def decode(encodedBytes: ImmutableBytes): GetBlockHashesByNumberMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val blockNumber = items.head.asPositiveLong
		val maxBlocks = items(1).asInt
		new GetBlockHashesByNumberMessage(blockNumber, maxBlocks)
	}
}
