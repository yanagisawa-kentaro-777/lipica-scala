package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.net.lpc.V0
import org.lipicalabs.lipica.core.net.lpc.sync.SyncStatistics
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 19:59
 * YANAGISAWA, Kentaro
 */
class LpcAdaptor extends Lpc {

	private val syncStats = new SyncStatistics

	override def hasStatusPassed = false

	override def isHashRetrievingDone = false

	override def bestKnownHash = ImmutableBytes.empty

	override def hasBlocksLack = false

	override def isIdle = true

	override def enableTransactions() = ()

	override def sendTransaction(tx: TransactionLike) = ()

	override def isHashRetrieving = false

	override def maxHashesAsk = 0

	override def hasStatusSucceeded = false

	override def changeState(newState: Any) = ()

	override def onSyncDone() = ()

	override def disableTransactions() = ()

	override def maxHashesAsk_=(v: Int) = ()

	override def onShutdown() = ()

	override def lastHashToAsk_=(v: ImmutableBytes) = ()

	override def lastHashToAsk = ImmutableBytes.empty

	override def getSyncStats = this.syncStats

	override def logSycStats() = ()

	override def version = V0
}
