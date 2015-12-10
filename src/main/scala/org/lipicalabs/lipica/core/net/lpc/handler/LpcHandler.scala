package org.lipicalabs.lipica.core.net.lpc.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.base.{BlockChain, TransactionLike}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.message.LpcMessage
import org.lipicalabs.lipica.core.net.lpc.sync.{SyncStatistics, Idle, SyncStateName, SyncQueue}
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

	protected var bestHash: ImmutableBytes = null
	protected var _lastHashToAsk: ImmutableBytes = null
	protected val _maxHashesAsk = SystemProperties.CONFIG.maxHashesAsk

	protected val sendHashes: mutable.Buffer[ImmutableBytes] = JavaConversions.asScalaBuffer(java.util.Collections.synchronizedList(new java.util.ArrayList[ImmutableBytes]))
	protected val syncStats: SyncStatistics = new SyncStatistics


	//TODO 未実装。
	override def channelRead0(channelHandlerContext: ChannelHandlerContext, i: LpcMessage) = ???

	override def hasStatusPassed = ???

	override def isHashRetrievingDone = ???

	override def bestKnownHash = ???

	override def hasBlocksLack = ???

	override def isIdle = ???

	override def enableTransactions() = ???

	override def sendTransaction(tx: TransactionLike) = ???

	override def isHashRetrieving = ???

	override def maxHashesAsk = ???

	override def hasStatusSucceeded = ???

	override def changeState(newState: Any) = ???

	override def onSyncDone() = ???

	override def disableTransactions() = ???

	override def onShutdown() = ???

	override def maxHashesAsk_=(v: Int) = ???

	override def lastHashToAsk_=(v: ImmutableBytes) = ???

	override def lastHashToAsk = ???

	override def logSycStats() = ???

	override def getSyncStats = ???

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