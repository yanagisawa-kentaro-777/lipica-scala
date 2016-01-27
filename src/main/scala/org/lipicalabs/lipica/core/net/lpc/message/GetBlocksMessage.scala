package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.crypto.digest.{Digest256, DigestValue}
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ダイジェスト値に対応したブロックを要求するメッセージです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:00
 * YANAGISAWA, Kentaro
 */
case class GetBlocksMessage(blockHashes: Seq[DigestValue]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.GetBlocks.asByte

	override def answerMessage: Option[Class[_ <: Message]] = Option(classOf[BlocksMessage])

	override def toString: String = "GetBlocksMessage(req=%,d)".format(this.blockHashes.size)
}

object GetBlocksMessage {

	def decode(encodedBytes: ImmutableBytes): GetBlocksMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new GetBlocksMessage(items.map(each => Digest256(each.bytes)))
	}
}