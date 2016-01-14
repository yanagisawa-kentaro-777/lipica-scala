package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.net.lpc.V0
import org.lipicalabs.lipica.core.net.lpc.message.{BlockHashesMessage, GetBlockHashesByNumberMessage}
import org.lipicalabs.lipica.core.sync.SyncStateName.DoneHashRetrieving
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:06
 * YANAGISAWA, Kentaro
 */
class Lpc0 extends LpcHandler(V0) {
	import Lpc0._

	private var lastAskedNumber: Long = 0L

	private var commonAncestorFound = false
	

	override protected def processBlockHashes(received: Seq[ImmutableBytes]): Unit = {
		if (logger.isTraceEnabled) {
			logger.trace("<Lpc0> BlockHashes received: Size=%,d".format(received.size))
		}
		if (received.isEmpty) {
			return
		}
		if (!this.commonAncestorFound) {
			maintainForkCoverage(received)
			return
		}
		this.syncQueue.addHashesLast(received)

		received.find(_ == this.lastHashToAsk).foreach {
			found => {
				changeState(DoneHashRetrieving)
				if (logger.isTraceEnabled) {
					logger.trace("<Lpc0> Peer %s: got terminal hash: %s".format(this.channel.nodeIdShort, this.lastHashToAsk))
				}
				return
			}
		}
		val blockNumber = this.lastAskedNumber + received.size
		sendGetBlockHashesByNumber(blockNumber, this.maxHashesAsk)
	}

	/**
	 * 渡されたブロック番号を最古（＝最小）のブロック番号とし、
	 * そこから指定された個数分だけ未来に向かう（＝ブロック番号を足し算する）ブロックの
	 * ダイジェスト値を要求するメッセージを送信します。
	 */
	private def sendGetBlockHashesByNumber(blockNumber: Long, maxHashesAsk: Int): Unit = {
		if (logger.isTraceEnabled) {
			logger.trace("<Lpc0> Peer %s: send GetBlockHashesByNumber: BlockNumber=%,d, MashHashesAsk=%,d".format(this.channel.nodeIdShort, blockNumber, maxHashesAsk))
		}
		sendMessage(GetBlockHashesByNumberMessage(blockNumber, maxHashesAsk))
		this.lastAskedNumber = blockNumber
	}

	override protected def processGetBlockHashesByNumber(message: GetBlockHashesByNumberMessage): Unit = {
		val hashes = this.blockchain.getSeqOfHashesStartingFromBlock(message.blockNumber, message.maxBlocks min NodeProperties.CONFIG.maxHashesAsk)
		sendMessage(BlockHashesMessage(hashes))
	}

	override def startHashRetrieving(): Unit = {
		this.commonAncestorFound = true
		val bestNumber = this.blockchain.bestBlock.blockNumber

		if (0L < bestNumber) {
			//genesis でない場合は、fork上だと悲観的に考える。
			startForkCoverage()
		} else {
			sendGetBlockHashesByNumber(bestNumber + 1, this.maxHashesAsk)
		}
	}

	private def startForkCoverage(): Unit = {
		this.commonAncestorFound = false
		if (logger.isTraceEnabled) {
			logger.trace("<Lpc0> Peer %s: start looking for common ancestor.".format(this.channel.nodeIdShort))
		}
		val bestNumber = this.blockchain.bestBlock.blockNumber
		val blockNumber = 1L max (bestNumber - ForkCoverBatchSize)
		sendGetBlockHashesByNumber(blockNumber, ForkCoverBatchSize)
	}

	private def maintainForkCoverage(received: Seq[ImmutableBytes]): Unit = {
		val blockNumber =
			if (1L < this.lastAskedNumber) {
				received.reverse.find(each => this.blockchain.existsBlock(each)).map {
					hash => {
						this.commonAncestorFound = true
						val block = this.blockchain.getBlockByHash(hash).get
						if (logger.isTraceEnabled) {
							logger.trace("<Lpc0> Peer %s: common ancestors found: BlockNumber=%,d, BlockHash=%s".format(this.channel.nodeIdShort, block.blockNumber, block.shortHash))
						}
						block.blockNumber
					}
				}.getOrElse(1L max (this.lastAskedNumber - ForkCoverBatchSize))
			} else {
				//いまGenesisしか持っていない。
				this.commonAncestorFound = true
				1L
			}
		if (this.commonAncestorFound) {
			sendGetBlockHashesByNumber(blockNumber, this.maxHashesAsk)
		} else {
			if (logger.isTraceEnabled) {
				logger.trace("<Lpc0> Peer %s: common ancestors not found yet.".format(this.channel.nodeIdShort))
			}
			sendGetBlockHashesByNumber(blockNumber, ForkCoverBatchSize)
		}
	}

}

object Lpc0 {
	private val logger = LoggerFactory.getLogger("sync")
	private val ForkCoverBatchSize = 512
}