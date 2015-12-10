package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.sync.{SyncStatistics, SyncStateName}
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

	def logSycStats(): Unit

	def changeState(newState: SyncStateName): Unit

	def hasBlocksLack: Boolean

	def isHashRetrievingDone: Boolean

	def isHashRetrieving: Boolean

	def isIdle: Boolean

	def maxHashesAsk_=(v: Int): Unit
	def maxHashesAsk: Int

	def lastHashToAsk_=(v: ImmutableBytes): Unit
	def lastHashToAsk: ImmutableBytes

	def bestKnownHash: ImmutableBytes

	def getSyncStats: SyncStatistics

	def enableTransactions(): Unit

	def disableTransactions(): Unit

	def sendTransaction(tx: TransactionLike): Unit

	def version: LpcVersion

	def onSyncDone(): Unit

}
