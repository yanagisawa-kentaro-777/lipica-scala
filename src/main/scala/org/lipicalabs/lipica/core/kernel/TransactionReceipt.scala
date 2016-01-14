package org.lipicalabs.lipica.core.kernel

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
class TransactionReceipt private(val logs: Seq[LogInfo], val bloomFilter: BloomFilter) {

	private val logsBuffer: mutable.Buffer[LogInfo] = new ArrayBuffer[LogInfo]

	private val transactionRef: AtomicReference[TransactionLike] = new AtomicReference[TransactionLike](null)
	def transaction: TransactionLike = this.transactionRef.get
	def transaction_=(v: TransactionLike): Unit = this.transactionRef.set(v)

	private val postTxStateRef: AtomicReference[ImmutableBytes] = new AtomicReference[ImmutableBytes](ImmutableBytes.empty)
	def postTxState: ImmutableBytes = this.postTxStateRef.get
	def postTxState_=(v: ImmutableBytes): Unit = this.postTxStateRef.set(v)

	private val manaUsedForTxRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def manaUsedForTx: BigInt = this.manaUsedForTxRef.get()
	def manaUsedForTx_=(v: BigInt): Unit = this.manaUsedForTxRef.set(v)

	private val cumulativeManaRef: AtomicReference[ImmutableBytes] = new AtomicReference[ImmutableBytes](ImmutableBytes.empty)
	def cumulativeMana: ImmutableBytes = this.cumulativeManaRef.get
	def cumulativeMana_=(v: ImmutableBytes): Unit = this.cumulativeManaRef.set(v)
	//def setCumulativeMana(v: Long): Unit = this.cumulativeMana = ImmutableBytes.asUnsignedByteArray(BigInt(v))

	def encode: ImmutableBytes = {
		val encodedPostTxState = RBACCodec.Encoder.encode(this.postTxState)
		val encodedCumulativeMana = RBACCodec.Encoder.encode(this.cumulativeMana)
		val encodedBloom = RBACCodec.Encoder.encode(this.bloomFilter.immutableBytes)
		val encodedLogInfoSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.logs.map(_.encode))

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedPostTxState, encodedCumulativeMana, encodedBloom, encodedLogInfoSeq))
	}

	override def toString: String = {
		"TxReceipt[PostTxState=%s, CumulativeMana=%s, BloomFilter=%s, LogsSize=%,d]".format(
			this.postTxState.toHexString, this.cumulativeMana.toHexString, this.bloomFilter.toString, this.logsBuffer.size
		)
	}

}

object TransactionReceipt {

	def apply(postTxState: ImmutableBytes, cumulativeMana: ImmutableBytes, logs: Seq[LogInfo]): TransactionReceipt = {
		val result = new TransactionReceipt(logs, buildBloomFilter(logs))
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

	def decode(encodedBytes: ImmutableBytes): TransactionReceipt = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val postTxState = items.head.bytes
		val cumulativeMana = items(1).bytes
		val bloomFilterBytes = items(2).bytes
		val logs = items(3).items.map(each => LogInfo.decode(each.items))

		val result = new TransactionReceipt(logs.toBuffer, BloomFilter(bloomFilterBytes.toByteArray))
		result.postTxState = postTxState
		result.cumulativeMana = cumulativeMana
		result
	}

	private def buildBloomFilter(logsSeq: Seq[LogInfo]): BloomFilter = {
		var result = BloomFilter()
		logsSeq.foreach {
			each => {
				result = result | each.createBloomFilter
			}
		}
		result
	}

}