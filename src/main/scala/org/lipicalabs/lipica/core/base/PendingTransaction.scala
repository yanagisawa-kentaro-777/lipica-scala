package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 11:14
 * YANAGISAWA, Kentaro
 */
class PendingTransaction private(val transaction: TransactionLike, val blockNumer: Long) {

	def hash: ImmutableBytes = this.transaction.hash

	def toBytes: ImmutableBytes = {
		val blockNumberBytes = BigInt(this.blockNumer).toByteArray
		val txBytes = this.transaction.toEncodedBytes.toByteArray
		val result = new Array[Byte](1 + blockNumberBytes.length + txBytes.length)
		result(0) = blockNumberBytes.length.toByte
		System.arraycopy(blockNumberBytes, 0, result, 1, blockNumberBytes.length)
		System.arraycopy(txBytes, 0, result, 1 + blockNumberBytes.length, txBytes.length)
		ImmutableBytes(result)
	}

	override def toString: String = {
		"PendingTransaction[tx=%s, blockNumber=%,d]".format(this.transaction, this.blockNumer)
	}

	override def equals(o: Any): Boolean = {
		try {
			this.transaction.hash == o.asInstanceOf[PendingTransaction].transaction.hash
		} catch {
			case e: Throwable =>
				false
		}
	}

	override def hashCode: Int = this.transaction.hashCode

}

object PendingTransaction {
	def apply(transaction: TransactionLike, blockNumer: Long): PendingTransaction = new PendingTransaction(transaction, blockNumer)

	def parse(encodedBytes: ImmutableBytes): PendingTransaction = {
		val blockNumberBytes = new Array[Byte](encodedBytes.head)
		val txBytes = new Array[Byte](encodedBytes.length - 1 - blockNumberBytes.length)

		System.arraycopy(encodedBytes, 1, blockNumberBytes, 0, blockNumberBytes.length)
		System.arraycopy(encodedBytes, 1 + blockNumberBytes.length, txBytes, 0, txBytes.length)

		new PendingTransaction(Transaction.decode(ImmutableBytes(txBytes)), BigInt(blockNumberBytes).longValue())
	}
}