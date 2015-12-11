package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.base.{Transaction, TransactionLike}
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:07
 * YANAGISAWA, Kentaro
 */
class TransactionsMessage(val transactions: Seq[TransactionLike]) extends LpcMessage {

	override def toEncodedBytes = {
		val seq = this.transactions.map(each => each.toEncodedBytes)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seq)
	}

	override def code = LpcMessageCode.Transactions.asByte

}

object TransactionsMessage {
	def decode(encodedBytes: ImmutableBytes): TransactionsMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val transactions = items.map(each => Transaction.decode(each.items))
		new TransactionsMessage(transactions)
	}
}