package org.lipicalabs.lipica.core.net.lpc.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.base.{BlockChain, TransactionLike}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.message.{TransactionsMessage, LpcMessage}
import org.lipicalabs.lipica.core.net.lpc.sync._
import org.lipicalabs.lipica.core.net.server.Channel
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:05
 * YANAGISAWA, Kentaro
 */
abstract class LpcHandler(override val version: LpcVersion) extends SimpleChannelInboundHandler[LpcMessage] with Lpc {

	import LpcHandler._
	//TODO auto wiring
	protected val blockchain: BlockChain = ???
	protected val syncQueue: SyncQueue = ???
	protected val worldManager: WorldManager = ???

	protected var channel: Channel = null
	private var messageQueue: MessageQueue = null

	protected var lpcState: LpcState = LpcState.Init

	protected var peerDiscoveryMode: Boolean = false

	private var blocksLackHits: Int = 0

	protected var syncState: SyncStateName = Idle
	protected var syncDone: Boolean = false


	protected var processTransactions: Boolean = true
	override def enableTransactions() = this.processTransactions = true
	override def disableTransactions() = this.processTransactions = false

	protected var bestHash: ImmutableBytes = null
	override def bestKnownHash = this.bestHash

	protected var _lastHashToAsk: ImmutableBytes = null
	override def lastHashToAsk = this._lastHashToAsk
	override def lastHashToAsk_=(v: ImmutableBytes) = this._lastHashToAsk = v

	protected var _maxHashesAsk = SystemProperties.CONFIG.maxHashesAsk
	override def maxHashesAsk = this._maxHashesAsk
	override def maxHashesAsk_=(v: Int) = this._maxHashesAsk = v

	protected val sentHashes: mutable.Buffer[ImmutableBytes] = JavaConversions.asScalaBuffer(java.util.Collections.synchronizedList(new java.util.ArrayList[ImmutableBytes]))
	protected val syncStats: SyncStatistics = new SyncStatistics


	//TODO 未実装。
	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: LpcMessage) = ???

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		loggerNet.warn("<LpcHandler> Exception caught.", cause)
		onShutdown()
		ctx.close()
	}

	override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
		loggerNet.debug("<LpcHandler> Handler removed.")
		onShutdown()
	}

	override def isHashRetrievingDone = this.syncState == DoneHashRetrieving

	override def isHashRetrieving = this.syncState == HashRetrieving

	override def hasBlocksLack = this.syncState == BlocksLack

	override def hasStatusPassed = this.lpcState != LpcState.Init

	override def hasStatusSucceeded = this.lpcState == LpcState.Succeeded

	override def onShutdown() = {
		changeState(Idle)
		returnHashes()
	}

	override def isIdle = ???

	protected def sendMessage(message: LpcMessage): Unit = {
		this.messageQueue.sendMessage(message)
		this.channel.nodeStatistics.lpcOutbound.add
	}

	override def sendTransaction(tx: TransactionLike) = {
		val message = new TransactionsMessage(Seq(tx))
		sendMessage(message)
	}


	override def changeState(newState: SyncStateName) = ???

	override def onSyncDone() = ???


	override def logSycStats() = ???

	override def getSyncStats = this.syncStats

	protected def returnHashes(): Unit = {
		this.sentHashes.synchronized {
			this.syncQueue.returnHashes(this.sentHashes.toIterable)
		}
		this.sentHashes.clear()
	}

}

object LpcHandler {
	private val loggerNet = LoggerFactory.getLogger("net")
	private val loggerSync = LoggerFactory.getLogger("sync")

	private val BlocksLackMaxHits = 5

}

sealed trait LpcState

object LpcState {
	case object Init extends LpcState
	case object Succeeded extends LpcState
	case object Failed extends LpcState
}