package org.lipicalabs.lipica.core.listener

import org.lipicalabs.lipica.core.base.{TransactionReceipt, Block, TransactionLike, TransactionExecutionSummary}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:34
 * YANAGISAWA, Kentaro
 */
trait LipicaListener {
	//TODO 未実装。
	def onTransactionExecuted(summary: TransactionExecutionSummary): Unit
	def onVMTraceCreated(txHash: String, trace: String): Unit
	def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]): Unit
	def trace(s: String): Unit
	def onBlock(block: Block, receipts: Iterable[TransactionReceipt]): Unit
}
