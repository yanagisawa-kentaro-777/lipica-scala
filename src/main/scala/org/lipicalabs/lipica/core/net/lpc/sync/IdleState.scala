package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.net.server.Channel

/**
  * Created by IntelliJ IDEA.
  * 2015/12/12 13:22
  * YANAGISAWA, Kentaro
  */
class IdleState extends AbstractSyncState(Idle) {

	 override def doOnTransition(): Unit = {
		 syncManager.pool.changeState(Idle)
	 }

	 override def doMaintain(): Unit = {
		 if (!syncManager.queue.isHashesEmpty) {
			 //新たなハッシュ値があるのだから、ダウンロードすべきである。
			 syncManager.changeState(BlockRetrieving)
		 } else if (syncManager.queue.isBlocksEmpty && !syncManager.isSyncDone) {
			 //まだ同期が完了していないが、キューは空である。
			 //ハッシュ値をダウンロードする。
			 syncManager.resetGapRecovery()
			 syncManager.changeState(HashRetrieving)
		 }
	 }

 }
