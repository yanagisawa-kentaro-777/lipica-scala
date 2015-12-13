package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.ParsedMessage
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:00
 * YANAGISAWA, Kentaro
 */
case class GetBlocksMessage(blockHashes: Seq[ImmutableBytes]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.GetBlocks.asByte

	override def answerMessage: Option[Class[_ <: ParsedMessage]] = Option(GetBlocksMessage.answerMessage)
}

object GetBlocksMessage {

	private val answerMessage = new BlocksMessage(Seq.empty).getClass

	def decode(encodedBytes: ImmutableBytes): GetBlocksMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new GetBlocksMessage(items.map(_.bytes))
	}
}