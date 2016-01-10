package org.lipicalabs.lipica.core.listener

import org.lipicalabs.lipica.core.base.{TransactionReceipt, Block, TransactionLike, TransactionExecutionSummary}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.peer_discovery.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 13:34
 * YANAGISAWA, Kentaro
 */
trait LipicaListener {

	/**
	 * ピアを発見した際に実行されます。
	 *
	 * @param node 発見されたピア。
	 */
	def onNodeDiscovered(node: Node): Unit

	/**
	 * 他のピア（P2Pネットワーク）との状態同期が完了した際に実行されます。
	 *
	 * 具体的には、最近生成されたばかりのブロックを受信した時点において
	 * このメソッドがコールされます。
	 */
	def onSyncDone(): Unit

	/**
	 * ブロックチェーンの先端にブロックが連結された際に実行されます。
	 * @param block 連結されたブロック。
	 * @param receipts ブロックに含まれるトランザクションが実行された後の状態。
	 */
	def onBlock(block: Block, receipts: Iterable[TransactionReceipt]): Unit

	def onTransactionExecuted(summary: TransactionExecutionSummary): Unit
	def onVMTraceCreated(txHash: String, trace: String): Unit
	def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]): Unit

	def onSendMessage(message: Message): Unit
	def onReceiveMessage(message: Message): Unit
	def onLpcStatusUpdated(node: Node, status: StatusMessage)

	def onHandshakePeer(node: Node, message: HelloMessage): Unit

	/**
	 * 一般的な情報を文字列として通知するために実行されます。
	 * @param message 通知メッセージ。
	 */
	def trace(message: String): Unit
}
