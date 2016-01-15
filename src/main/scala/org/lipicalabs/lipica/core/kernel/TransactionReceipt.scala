package org.lipicalabs.lipica.core.kernel

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{Digest256, DigestValue, UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.LogInfo

/**
 * トランザクション実行後の情報の組み合わせクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:02
 * YANAGISAWA, Kentaro
 */
class TransactionReceipt private(val logs: Seq[LogInfo], val bloomFilter: BloomFilter, val cumulativeMana: ImmutableBytes, val postTxState: DigestValue) {

	private val transactionRef: AtomicReference[TransactionLike] = new AtomicReference[TransactionLike](null)
	def transaction: TransactionLike = this.transactionRef.get
	def transaction_=(v: TransactionLike): Unit = this.transactionRef.set(v)

	private val manaUsedForTxRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def manaUsedForTx: BigInt = this.manaUsedForTxRef.get()
	def manaUsedForTx_=(v: BigInt): Unit = this.manaUsedForTxRef.set(v)

	def encode: ImmutableBytes = {
		val encodedPostTxState = RBACCodec.Encoder.encode(this.postTxState)
		val encodedCumulativeMana = RBACCodec.Encoder.encode(this.cumulativeMana)
		val encodedBloom = RBACCodec.Encoder.encode(this.bloomFilter.immutableBytes)
		val encodedLogInfoSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(this.logs.map(_.encode))

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedPostTxState, encodedCumulativeMana, encodedBloom, encodedLogInfoSeq))
	}

	/**
	 * このトランザクションの実行者自身が出力したのでないログを
	 * 排除した TransactionReceipt のインスタンスを生成して返します。
	 *
	 * Call や CallCode に伴うログ出力の扱いが不明であるため、それに対処するためのメソッドです。
	 */
	def excludeAlienLogs: TransactionReceipt = {
		val tx = this.transaction
		if (tx eq null) {
			return this
		}
		val filteredLogs = this.logs.filter(each => each.address == tx.senderAddress)
		val result = TransactionReceipt.apply(filteredLogs, this.cumulativeMana, this.postTxState)
		result.transaction = tx
		result.manaUsedForTx = this.manaUsedForTx
		result
	}

	override def toString: String = {
		"TxReceipt[Tx=%s, PostTxState=%s, CumulativeMana=%s, BloomFilter=%s, LogsSize=%,d]".format(
			this.transaction.hash, this.postTxState.toHexString, this.cumulativeMana.toHexString, this.bloomFilter.toString, this.logs.size
		)
	}

}

object TransactionReceipt {

	def apply(logs: Seq[LogInfo], cumulativeMana: ImmutableBytes, postTxState: DigestValue): TransactionReceipt = {
		new TransactionReceipt(logs, buildBloomFilter(logs), cumulativeMana, postTxState)
	}

	def decode(encodedBytes: ImmutableBytes): TransactionReceipt = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val postTxState = items.head.bytes
		val cumulativeMana = items(1).bytes
		val bloomFilterBytes = items(2).bytes
		val logs = items(3).items.map(each => LogInfo.decode(each.items))

		new TransactionReceipt(logs.toBuffer, BloomFilter(bloomFilterBytes.toByteArray), cumulativeMana, Digest256(postTxState))
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