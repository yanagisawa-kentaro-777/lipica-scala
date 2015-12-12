package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.base.BlockWrapper
import org.lipicalabs.lipica.core.net.server.Channel
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:51
 * YANAGISAWA, Kentaro
 */
class SyncManager {
	//TODO 未実装。

	def pool: PeersPool = ???

	def queue: SyncQueue = ???

	def changeState(stateName: SyncStateName): Unit = ???

	def isSyncDone: Boolean = ???

	def resetGapRecovery(): Unit = ???

	def isPeerStuck(peer: Channel): Boolean = ???

	def getGapBlock: BlockWrapper = ???

	def startMaster(master: Channel): Unit = ???

	def tryGapRecovery(blockWrapper: BlockWrapper): Unit = ???

	def reportInvalidBlock(nodeId: ImmutableBytes): Unit = ???

	def notifyNewBlockImported(blockWrapper: BlockWrapper): Unit = ???

}
