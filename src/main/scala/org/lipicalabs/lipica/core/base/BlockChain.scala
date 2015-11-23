package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.ImportResult
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 16:28
 * YANAGISAWA, Kentaro
 */
trait BlockChain {

	def size: Long

	def append(block: Block): Unit

	def tryToConnect(block: Block): ImportResult

	def storeBlock(block: Block, receipts: Seq[TransactionReceipt]): Unit

	def existsBlock(hash: ImmutableBytes): Boolean
	def getBlockByNumber(blockNumber: Long): Option[Block]
	def getBlockByHash(hash: ImmutableBytes): Option[Block]

	def bestBlock: Block
	def bestBlock_=(block: Block): Unit

	def bestBlockHash: ImmutableBytes

	def getSeqOfHashesStartingFrom(hash: ImmutableBytes, count: Int): Seq[ImmutableBytes]

	def getSeqOfHashesStartingFromBlock(blockNumber: Long, count: Int): Seq[ImmutableBytes]

	def getTransactionReceiptByHash(hash: ImmutableBytes): Option[TransactionReceipt]

	def hasParentOnTheChain(block: Block): Boolean

	def updateTotalDifficulty(block: Block): Unit

	def totalDifficulty: BigInt
	def totalDifficulty_=(v: BigInt): Unit

	def allChains: Iterable[Chain]
	def garbage: Iterable[Block]

	def pendingTransactions: Set[TransactionLike]
	def addPendingTransactions(transactions: Set[TransactionLike]): Unit
	def clearPendingTransactions(receivedTransactions: Iterable[TransactionLike]): Unit

	def exitOn: Long
	def exitOn_=(v: Long): Unit

	def close(): Unit

}
