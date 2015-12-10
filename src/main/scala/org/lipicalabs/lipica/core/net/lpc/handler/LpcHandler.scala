package org.lipicalabs.lipica.core.net.lpc.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.message.LpcMessage
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:05
 * YANAGISAWA, Kentaro
 */
abstract class LpcHandler(override val version: LpcVersion) extends SimpleChannelInboundHandler[LpcMessage] with Lpc {

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
