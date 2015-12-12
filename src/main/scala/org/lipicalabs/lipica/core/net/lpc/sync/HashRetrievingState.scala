package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.net.server.Channel
import org.slf4j.LoggerFactory

/**
  * Created by IntelliJ IDEA.
  * 2015/12/12 13:22
  * YANAGISAWA, Kentaro
  */
class HashRetrievingState extends AbstractSyncState(HashRetrieving) {

	import HashRetrievingState._

	 override def doMaintain(): Unit = {
		 var master: Channel = null
		 val foundOrNone = syncManager.pool.peers.find(peer => peer.isHashRetrievingDone || peer.isHashRetrieving)
		 foundOrNone.foreach {
			 peer => {
				 if (peer.isHashRetrievingDone) {
					 //次はブロックを取得する。
					 syncManager.changeState(BlockRetrieving)
					 return
				 } else {
					master = peer
				 }
			 }
		 }
		 if (Option(master).isDefined) {
			 if (syncManager.isPeerStuck(master)) {
				 syncManager.pool.ban(master)
				 //いちおう、取得できるブロックがないかどうか確認した上で、HashRetrievingを続ける。
				 syncManager.changeState(BlockRetrieving)
				 return
			 }
		 } else {
			if (logger.isTraceEnabled) {
				logger.trace("<HashRetrievingState> Hash retrieving is in progress.")
			}
			 if (syncManager.getGapBlock ne null) {
				 master = syncManager.pool.getByNodeId(syncManager.getGapBlock.nodeId).orNull
			 }
			 if (master eq null) {
				 master = syncManager.pool.getBest.orNull
			 }
			 if (master eq null) {
				 return
			 }
			 syncManager.startMaster(master)
		 }
		 syncManager.pool.changeState(BlockRetrieving, (peer) => peer.isIdle)
	 }
 }

object HashRetrievingState {
	private val logger = LoggerFactory.getLogger("sync")
}
