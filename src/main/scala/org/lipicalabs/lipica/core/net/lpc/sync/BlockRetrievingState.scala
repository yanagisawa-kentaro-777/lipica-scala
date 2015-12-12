package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.net.server.Channel

/**
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:22
 * YANAGISAWA, Kentaro
 */
class BlockRetrievingState extends AbstractSyncState(BlockRetrieving) {

	override def doOnTransition(): Unit = {
		syncManager.pool.changeState(BlockRetrieving)
	}

	override def doMaintain(): Unit = {
		if (syncManager.queue.isHashesEmpty) {
			syncManager.changeState(Idle)
		} else {
			syncManager.pool.changeState(BlockRetrieving, (peer: Channel) => peer.isIdle)
		}
	}

}
