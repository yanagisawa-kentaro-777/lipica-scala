package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.crypto.digest.{EmptyDigest, DigestValue}
import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.net.lpc.V0
import org.lipicalabs.lipica.core.sync.{SyncStateName, SyncStatistics}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 19:59
 * YANAGISAWA, Kentaro
 */
class LpcAdaptor extends Lpc {

	private val syncStats = new SyncStatistics

	override def hasStatusPassed = false

	override def isHashRetrievingDone = false

	override def bestKnownHash = EmptyDigest

	override def hasBlocksLack = false

	override def isIdle = true

	override def enableTransactions() = ()

	override def sendTransaction(tx: TransactionLike) = ()

	override def isHashRetrieving = false

	override def maxHashesAsk = 0

	override def hasStatusSucceeded = false

	override def changeState(newState: SyncStateName) = ()

	override def onSyncDone() = ()

	override def disableTransactions() = ()

	override def maxHashesAsk_=(v: Int) = ()

	override def onShutdown() = ()

	override def lastHashToAsk_=(v: DigestValue) = ()

	override def getSyncState: SyncStateName = SyncStateName.Idle

	override def syncStateSummaryAsString: String = ""

	override def lastHashToAsk = EmptyDigest

	override def getSyncStats = this.syncStats

	override def logSyncStats() = ()

	override def version = V0
}
