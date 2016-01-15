package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.sync.{SyncStatistics, SyncStateName}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 19:53
 * YANAGISAWA, Kentaro
 */
trait Lpc {

	def hasStatusPassed: Boolean

	def hasStatusSucceeded: Boolean

	def onShutdown(): Unit

	def logSyncStats(): Unit

	def changeState(newState: SyncStateName): Unit

	def hasBlocksLack: Boolean

	def isHashRetrievingDone: Boolean

	def isHashRetrieving: Boolean

	def isIdle: Boolean

	def maxHashesAsk_=(v: Int): Unit
	def maxHashesAsk: Int

	def lastHashToAsk_=(v: DigestValue): Unit
	def lastHashToAsk: DigestValue

	def bestKnownHash: DigestValue

	def getSyncState: SyncStateName

	def getSyncStats: SyncStatistics

	def syncStateSummaryAsString: String

	def enableTransactions(): Unit

	def disableTransactions(): Unit

	def sendTransaction(tx: TransactionLike): Unit

	def version: LpcVersion

	def onSyncDone(): Unit

}
