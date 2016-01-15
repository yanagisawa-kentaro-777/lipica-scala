package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.utils.UtilConsts
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.InternalTransaction

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 12:36
 * YANAGISAWA, Kentaro
 */
class TransactionExecutionSummary private (
	val transactionHash: DigestValue,
	val value: BigInt,
	val manaPrice: BigInt,
	val manaLimit: BigInt,
	val manaUsed: BigInt,
	val manaLeftOver: BigInt,
	val manaRefund: BigInt,
	val deletedAccounts: Iterable[DataWord],
	val internalTransactions: Seq[InternalTransaction],
	val failed: Boolean
) {
	private def calculateCost(mana: BigInt): BigInt = this.manaPrice * mana

	def calculateFee: BigInt = calculateCost(this.manaLimit - (this.manaLeftOver + this.manaRefund))

	def calculateRefund: BigInt = calculateCost(this.manaRefund)

	def calculateLeftOver: BigInt = calculateCost(this.manaLeftOver)

}

object TransactionExecutionSummary {

	class Builder(private val tx: TransactionLike) {
		private val transactionHash = this.tx.hash
		private val manaLimit = this.tx.manaLimit.toPositiveBigInt
		private val manaPrice = this.tx.manaPrice.toPositiveBigInt
		private val value = this.tx.value.toPositiveBigInt

		private var manaUsed: BigInt = UtilConsts.Zero
		def manaUsed(v: BigInt): Builder = {
			this.manaUsed = v
			this
		}

		private var manaLeftOver: BigInt = UtilConsts.Zero
		def manaLeftOver(v: BigInt): Builder = {
			this.manaLeftOver = v
			this
		}

		private var manaRefund: BigInt = UtilConsts.Zero
		def manaRefund(v: BigInt): Builder = {
			this.manaRefund= v
			this
		}

		private var internalTransactions: Seq[InternalTransaction] = Seq.empty
		def internalTransactions(v: Seq[InternalTransaction]): Builder = {
			this.internalTransactions = v
			this
		}

		private var deletedAccounts: Iterable[DataWord] = Seq.empty
		def deletedAccounts(v: Iterable[DataWord]): Builder = {
			this.deletedAccounts = v
			this
		}

		private var failed: Boolean = false
		def markAsFailed: Builder = {
			this.failed = true
			this
		}

		def build: TransactionExecutionSummary = {
			val result = new TransactionExecutionSummary(
				transactionHash = this.transactionHash,
				value = this.value,
				manaPrice = this.manaPrice,
				manaLimit = this.manaLimit,
				manaUsed = this.manaUsed,
				manaLeftOver = this.manaLeftOver,
				manaRefund = this.manaRefund,
				deletedAccounts = this.deletedAccounts,
				internalTransactions = this.internalTransactions,
				failed = this.failed
			)
			if (result.failed) {
				result.internalTransactions.foreach(_.reject())
			}
			result
		}
	}

	def builder(tx: TransactionLike): Builder = new Builder(tx)

}