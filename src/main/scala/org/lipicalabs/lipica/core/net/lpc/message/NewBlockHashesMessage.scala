package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:02
 * YANAGISAWA, Kentaro
 */
case class NewBlockHashesMessage(blockHashes: Seq[ImmutableBytes]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.NewBlockHashes.asByte
}

object NewBlockHashesMessage {
	def decode(encodedBytes: ImmutableBytes): NewBlockHashesMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new NewBlockHashesMessage(items.map(_.bytes))
	}
}