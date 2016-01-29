package org.lipicalabs.lipica.core.facade.listener

import org.lipicalabs.lipica.core.kernel.{TransactionReceipt, Block, TransactionLike, TransactionExecutionSummary}
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.peer_discovery.Node

/**
 * 外部アプリケーションが重要なイベントを捕捉するために実装すべきtraitです。
 *
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

	/**
	 * 自ノード上でトランザクションが実行された際に呼びだされます。
	 */
	def onTransactionExecuted(summary: TransactionExecutionSummary): Unit

	/**
	 * Transactionsメッセージでトランザクションを受信した際に呼び出されます。
	 */
	def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]): Unit

	/**
	 * メッセージ送信時に呼び出されます。
	 */
	def onSendMessage(message: Message): Unit

	/**
	 * メッセージ受信時に呼び出されます。
	 */
	def onReceiveMessage(message: Message): Unit

	/**
	 * Statusメッセージの受信時に呼び出されます。
	 */
	def onLpcStatusUpdated(node: Node, status: StatusMessage)

	/**
	 * P2Pハンドシェイク完了時に呼び出されます。
	 */
	def onHandshakePeer(node: Node, message: HelloMessage): Unit

	/**
	 * 一般的な情報を文字列として通知するために実行されます。
	 * @param message 通知メッセージ。
	 */
	def trace(message: String): Unit

	/**
	 * VMの詳細出力が実行された際に呼び出されます。
	 */
	def onVMTraceCreated(txHash: String, trace: String): Unit
}
