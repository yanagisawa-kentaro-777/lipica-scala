package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.ImportResult
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 16:28
 * YANAGISAWA, Kentaro
 */
trait BlockChain {

	/**
	 * このチェーンに対して、渡されたブロックを連結することが可能であれば連結しようとします。
	 */
	def tryToConnect(block: Block): ImportResult

	/**
	 * このチェーンの末尾にブロックを加えます。
	 */
	def append(block: Block): Unit

	def storeBlock(block: Block, receipts: Seq[TransactionReceipt]): Unit

	/**
	 * このチェーンにおける最新のブロック。
	 */
	def bestBlock: Block
	def bestBlock_=(block: Block): Unit

	/**
	 * このチェーンにおける最新のブロックのダイジェスト値を返します。
	 */
	def bestBlockHash: ImmutableBytes

	/**
	 * ブロックチェーンに属する全ブロックのdifficultyの合計値。
	 * フォークしたチェーンどうしの優劣をけっていするために利用。
	 */
	def totalDifficulty: BigInt
	def totalDifficulty_=(v: BigInt): Unit

	/**
	 * このチェーンに登録されたブロックの数＝最大のブロック番号＋１を返します。
	 */
	def size: Long

	/**
	 * 指定されたブロックが、このチェーン上に存在するか否かを返します。
	 */
	def existsBlock(hash: ImmutableBytes): Boolean

	/**
	 * 指定されたブロック番号のブロックがこのチェーン上に存在すれば、そのブロックを返します。
	 */
	def getBlockByNumber(blockNumber: Long): Option[Block]

	/**
	 * 指定されたハッシュ値のブロックがこのチェーン上に存在すれば、そのブロックを返します。
	 */
	def getBlockByHash(hash: ImmutableBytes): Option[Block]


	def getSeqOfHashesStartingFrom(hash: ImmutableBytes, count: Int): Seq[ImmutableBytes]

	def getSeqOfHashesStartingFromBlock(blockNumber: Long, count: Int): Seq[ImmutableBytes]

	def getTransactionReceiptByHash(hash: ImmutableBytes): Option[TransactionReceipt]

	def hasParentOnTheChain(block: Block): Boolean

	def updateTotalDifficulty(block: Block): Unit

	def altChains: Iterable[Chain]
	def garbage: Iterable[Block]

	def pendingTransactions: Set[TransactionLike]
	def addPendingTransactions(transactions: Set[TransactionLike]): Unit
	def clearPendingTransactions(receivedTransactions: Iterable[TransactionLike]): Unit

	def exitOn: Long
	def exitOn_=(v: Long): Unit

	def close(): Unit

}
