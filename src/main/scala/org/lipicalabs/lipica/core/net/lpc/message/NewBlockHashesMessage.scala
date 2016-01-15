package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{Digest256, DigestValue, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:02
 * YANAGISAWA, Kentaro
 */
case class NewBlockHashesMessage(blockHashes: Seq[DigestValue]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.NewBlockHashes.asByte

	override def toString: String = "NewBlockHashesMessage(%,d hashes)".format(this.blockHashes.size)
}

object NewBlockHashesMessage {
	def decode(encodedBytes: ImmutableBytes): NewBlockHashesMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new NewBlockHashesMessage(items.map(each => Digest256(each.bytes)))
	}
}