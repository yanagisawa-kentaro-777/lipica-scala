package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * 送信元ノードが知っているブロックダイジェスト値を、
 * 送信先ノードに通知するためのメッセージです。
 *
 * GetBlockHashes や GetBlockHashesByNumber を受信した際に送信され、
 * 受信側ではSyncQueueに登録されます。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/09 20:36
 * YANAGISAWA, Kentaro
 */
case class BlockHashesMessage(blockHashes: Seq[ImmutableBytes]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.blockHashes.map(each => RBACCodec.Encoder.encode(each))
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.BlockHashes.asByte

	override def toString: String = "BlockHashesMessage(%,d hashes)".format(this.blockHashes.size)

}

object BlockHashesMessage {

	def decode(encodedBytes: ImmutableBytes): BlockHashesMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		new BlockHashesMessage(items.map(_.bytes))
	}

}