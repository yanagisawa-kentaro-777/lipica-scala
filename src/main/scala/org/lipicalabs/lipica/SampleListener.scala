package org.lipicalabs.lipica

import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.facade.listener.LipicaListener
import org.lipicalabs.lipica.core.kernel.{TransactionReceipt, Block, TransactionExecutionSummary, TransactionLike}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.peer_discovery.Node

class SampleListener(lipica: Lipica) extends LipicaListener {

	override def onNodeDiscovered(node: Node) = {
		println("Peer: Discovered " + node.id.toShortString)
	}

	override def onSyncDone() = {
		println()
		println("Sync done.")
		println()

		//val manaPrice = lipica.recentManaPrice
		//lipica.repository.getBalance() etc.
		//lipica.submitTransaction()

	}

	override def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]) = {
		println("Tx: %,d txs received.".format(transactions.size))
	}

	override def onBlock(block: Block, receipts: Iterable[TransactionReceipt]) = {
		println("Block: Connected %,d".format(block.blockNumber))
	}

	override def onLpcStatusUpdated(node: Node, status: StatusMessage) = ()

	override def onTransactionExecuted(summary: TransactionExecutionSummary) = ()

	override def onHandshakePeer(node: Node, message: HelloMessage) = ()

	override def trace(message: String) = ()

	override def onReceiveMessage(message: Message) = ()

	override def onVMTraceCreated(txHash: String, trace: String) = ()

	override def onSendMessage(message: Message) = ()

}
