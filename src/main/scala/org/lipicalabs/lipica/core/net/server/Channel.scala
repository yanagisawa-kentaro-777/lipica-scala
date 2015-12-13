package org.lipicalabs.lipica.core.net.server

import org.lipicalabs.lipica.core.net.lpc.sync.{SyncStatistics, SyncStateName}
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.net.transport.discover.NodeStatistics
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:55
 * YANAGISAWA, Kentaro
 */
class Channel {
	//TODO 未実装。

	def nodeId: ImmutableBytes = ???
	def peerId: String = ???
	def peerIdShort: String = ???
	def node: Node = ???
	def nodeStatistics: NodeStatistics = ???

	def bestKnownHash: ImmutableBytes = ???
	def lastHashToAsk: ImmutableBytes = ???
	def lastHashToAsk_=(v: ImmutableBytes): Unit = ???
	def maxHashesAsk: Int = ???

	def isIdle: Boolean = ???
	def isHashRetrieving: Boolean = ???
	def isHashRetrievingDone: Boolean = ???

	def hasBlocksLack: Boolean = ???

	def changeSyncState(state: SyncStateName): Unit = ???

	def logSyncStats(): Unit = ???

	def getSyncStats: SyncStatistics = ???

	def totalDifficulty: BigInt = this.nodeStatistics.lpcTotalDifficulty

}
