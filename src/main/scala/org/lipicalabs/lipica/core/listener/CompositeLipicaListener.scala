package org.lipicalabs.lipica.core.listener

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.base.{TransactionExecutionSummary, TransactionReceipt, Block, TransactionLike}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.transport.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 15:42
 * YANAGISAWA, Kentaro
 */
class CompositeLipicaListener extends LipicaListener {

	private val listenersRef = new AtomicReference[Seq[LipicaListener]](Seq.empty)

	def addListener(listener: LipicaListener): Unit = {
		this.synchronized {
			val current = listeners
			this.listenersRef.set(listener +: current)
		}
	}

	def listeners: Seq[LipicaListener] = this.listenersRef.get

	override def onTransactionExecuted(summary: TransactionExecutionSummary) = listeners.foreach(_.onTransactionExecuted(summary))

	override def onBlock(block: Block, receipts: Iterable[TransactionReceipt]) = listeners.foreach(_.onBlock(block, receipts))

	override def onLpcStatusUpdated(node: Node, status: StatusMessage) = listeners.foreach(_.onLpcStatusUpdated(node, status))

	override def onNodeDiscovered(n: Node) = listeners.foreach(_.onNodeDiscovered(n))

	override def onSyncDone() = listeners.foreach(_.onSyncDone())

	override def onHandshakePeer(node: Node, message: HelloMessage) = listeners.foreach(_.onHandshakePeer(node, message))

	override def trace(s: String) = listeners.foreach(_.trace(s))

	override def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]) = listeners.foreach(_.onPendingTransactionsReceived(transactions))

	override def onReceiveMessage(message: Message) = listeners.foreach(_.onReceiveMessage(message))

	override def onVMTraceCreated(txHash: String, trace: String) = listeners.foreach(_.onVMTraceCreated(txHash, trace))

	override def onSendMessage(message: Message) = listeners.foreach(_.onSendMessage(message))
}
