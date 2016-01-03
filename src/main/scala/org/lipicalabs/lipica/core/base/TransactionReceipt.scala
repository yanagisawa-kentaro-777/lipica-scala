package org.lipicalabs.lipica.core.base

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.LogInfo

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * トランザクション実行後の情報の組み合わせクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:02
 * YANAGISAWA, Kentaro
 */
class TransactionReceipt private(private var _bloomFilter: Bloom, logs: Seq[LogInfo]) {

	private val logsBuffer: mutable.Buffer[LogInfo] = new ArrayBuffer[LogInfo]

	private var _transaction: TransactionLike = null
	def transaction: TransactionLike = this._transaction
	def transaction_=(v: TransactionLike): Unit = this._transaction = v

	private var _postTxState: ImmutableBytes = ImmutableBytes.empty
	def postTxState: ImmutableBytes = this._postTxState
	def postTxState_=(v: ImmutableBytes): Unit = this._postTxState = v

	private val manaUsedForTxRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def manaUsedForTx: BigInt = this.manaUsedForTxRef.get()
	def manaUsedForTx_=(v: BigInt): Unit = this.manaUsedForTxRef.set(v)

	private var _cumulativeMana: ImmutableBytes = ImmutableBytes.empty
	def cumulativeMana: ImmutableBytes = this._cumulativeMana
	def cumulativeMana_=(v: ImmutableBytes): Unit = this._cumulativeMana = v
	def setCumulativeMana(v: Long): Unit = this.cumulativeMana = ImmutableBytes.asUnsignedByteArray(BigInt(v))


	def bloomFilter: Bloom = this._bloomFilter
	def logsAsSeq: Seq[LogInfo] = this.logsBuffer.toSeq

	def setLogs(seq: Seq[LogInfo]): Unit = {
		this.logsBuffer.clear()
		this.logsBuffer.appendAll(seq)

		this.logsBuffer.foreach {
			each => {
				this._bloomFilter = this._bloomFilter | each.createBloomFilter
			}
		}
	}

	def encode: ImmutableBytes = {
		val encodedPostTxState = RBACCodec.Encoder.encode(this.postTxState)
		val encodedCumulativeMana = RBACCodec.Encoder.encode(this.cumulativeMana)
		val encodedBloom = RBACCodec.Encoder.encode(this.bloomFilter.immutableBytes)
		val encodedLogInfoSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.logsAsSeq.map(_.encode))

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedPostTxState, encodedCumulativeMana, encodedBloom, encodedLogInfoSeq))
	}

	override def toString: String = {
		"TransactionReceipt[postTxState=%s, cumulativeMana=%s, bloom=%s, logsSize=%,d]".format(
			this.postTxState.toHexString, this.cumulativeMana.toHexString, this.bloomFilter.toString, this.logsBuffer.size
		)
	}

	setLogs(logs)

}

object TransactionReceipt {

	def apply(postTxState: ImmutableBytes, cumulativeMana: ImmutableBytes, bloom: Bloom, logs: Seq[LogInfo]): TransactionReceipt = {
		val result = new TransactionReceipt(bloom, logs)
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

	def decode(encodedBytes: ImmutableBytes): TransactionReceipt = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val postTxState = items.head.bytes
		val cumulativeMana = items(1).bytes
		val bloom = items(2).bytes
		val logs = items(3).items.map(each => LogInfo.decode(each.items))

		val result = new TransactionReceipt(Bloom(bloom.toByteArray), logs.toBuffer)
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

}