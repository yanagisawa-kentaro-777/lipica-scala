package org.lipicalabs.lipica.core.listener

import org.lipicalabs.lipica.core.base.{TransactionExecutionSummary, TransactionReceipt, Block, TransactionLike}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.transport.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 15:27
 * YANAGISAWA, Kentaro
 */
class LipicaListenerAdaptor extends LipicaListener {

	override def onTransactionExecuted(summary: TransactionExecutionSummary) = ()

	override def onBlock(block: Block, receipts: Iterable[TransactionReceipt]) = ()

	override def onLpcStatusUpdated(node: Node, status: StatusMessage) = ()

	override def onNodeDiscovered(n: Node) = ()

	override def onSyncDone() = ()

	override def onHandshakePeer(node: Node, message: HelloMessage) = ()

	override def trace(s: String) = ()

	override def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]) = ()

	override def onReceiveMessage(message: Message) = ()

	override def onVMTraceCreated(txHash: String, trace: String) = ()

	override def onSendMessage(message: Message) = ()
}
