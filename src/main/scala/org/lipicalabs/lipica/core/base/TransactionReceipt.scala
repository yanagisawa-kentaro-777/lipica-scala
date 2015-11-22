package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.LogInfo

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:02
 * YANAGISAWA, Kentaro
 */
class TransactionReceipt private(private var _bloomFilter: Bloom, private val logInfoBuffer: mutable.Buffer[LogInfo]) {

	private var _transaction: TransactionLike = null
	def transaction: TransactionLike = this._transaction
	def transaction_=(v: TransactionLike): Unit = this._transaction = v

	private var _postTxState: ImmutableBytes = ImmutableBytes.empty
	def postTxState: ImmutableBytes = this._postTxState
	def postTxState_=(v: ImmutableBytes): Unit = this._postTxState = v

	private var _cumulativeMana: ImmutableBytes = ImmutableBytes.empty
	def cumulativeMana: ImmutableBytes = this._cumulativeMana
	def cumulativeMana_=(v: ImmutableBytes): Unit = this._cumulativeMana = v
	def setCumulativeMana(v: Long): Unit = this.cumulativeMana = ImmutableBytes.asUnsignedByteArray(BigInt(v))


	def bloomFilter: Bloom = this._bloomFilter
	def logInfoSeq: Seq[LogInfo] = this.logInfoBuffer.toSeq

	def setLogInfoSeq(seq: Seq[LogInfo]): Unit = {
		this.logInfoBuffer.clear()
		this.logInfoBuffer.appendAll(seq)

		this.logInfoBuffer.foreach {
			each => {
				this._bloomFilter = this._bloomFilter | each.getBloom
			}
		}
	}

	def encode: ImmutableBytes = {
		val encodedPostTxState = RBACCodec.Encoder.encode(this.postTxState)
		val encodedCumulativeMana = RBACCodec.Encoder.encode(this.cumulativeMana)
		val encodedBloom = RBACCodec.Encoder.encode(this.bloomFilter.immutableBytes)
		val encodedLogInfoSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.logInfoSeq.map(_.getEncoded))

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedPostTxState, encodedCumulativeMana, encodedBloom, encodedLogInfoSeq))
	}

	override def toString: String = {
		"TransactionReceipt[postTxState=%s, cumulativeMana=%s, bloom=%s, logsSize=%,d]".format(
			this.postTxState.toHexString, this.cumulativeMana.toHexString, this.bloomFilter.toString, this.logInfoBuffer.size
		)
	}

}

object TransactionReceipt {

	def apply(postTxState: ImmutableBytes, cumulativeMana: ImmutableBytes, bloom: Bloom, logInfos: Seq[LogInfo]): TransactionReceipt = {
		val result = new TransactionReceipt(bloom, logInfos.toBuffer)
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

	def decode(encodedByts: ImmutableBytes): TransactionReceipt = {
		val items = RBACCodec.Decoder.decode(encodedByts).right.get.items
		val postTxState = items.head.bytes
		val cumulativeMana = items(1).bytes
		val bloom = items(2).bytes
		val logs = items(3).items.map(each => LogInfo.decode(each.items))

		val result = new TransactionReceipt(Bloom.create(bloom), logs.toBuffer)
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

}