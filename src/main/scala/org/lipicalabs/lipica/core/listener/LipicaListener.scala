package org.lipicalabs.lipica.core.listener

import org.lipicalabs.lipica.core.base.{TransactionReceipt, Block, TransactionLike, TransactionExecutionSummary}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.transport.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:34
 * YANAGISAWA, Kentaro
 */
trait LipicaListener {
	def onTransactionExecuted(summary: TransactionExecutionSummary): Unit
	def onVMTraceCreated(txHash: String, trace: String): Unit
	def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]): Unit
	def trace(s: String): Unit
	def onBlock(block: Block, receipts: Iterable[TransactionReceipt]): Unit
	def onSendMessage(message: Message): Unit
	def onReceiveMessage(message: Message): Unit
	def onLpcStatusUpdated(node: Node, status: StatusMessage)
	def onSyncDone(): Unit
	def onHandshakePeer(node: Node, message: HelloMessage): Unit
	def onNodeDiscovered(n: Node): Unit
}
