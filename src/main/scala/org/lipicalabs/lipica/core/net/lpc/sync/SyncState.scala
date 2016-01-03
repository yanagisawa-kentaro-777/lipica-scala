package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.net.channel.Channel
import org.slf4j.LoggerFactory

sealed trait SyncStateName

object SyncStateName {
	case object Idle extends SyncStateName
	case object HashRetrieving extends SyncStateName
	case object BlockRetrieving extends SyncStateName
	case object DoneHashRetrieving extends SyncStateName
	case object BlocksLack extends SyncStateName
}


/**
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:11
 * YANAGISAWA, Kentaro
 */
trait SyncState {
	def is(name: SyncStateName): Boolean
	def doOnTransition(): Unit
	def doMaintain(): Unit
}

abstract class AbstractSyncState(val name: SyncStateName) extends SyncState {

	private var _syncManager: SyncManager = null
	def syncManager: SyncManager = this._syncManager
	def syncManager_=(v: SyncManager): Unit = this._syncManager = v

	override def is(aName: SyncStateName): Boolean = this.name == aName
	override def doOnTransition(): Unit = {
		//
	}
	override def doMaintain(): Unit = {
		//
	}
	override def toString: String = this.name.toString
}

class IdleState extends AbstractSyncState(SyncStateName.Idle) {

	override def doOnTransition(): Unit = {
		syncManager.pool.changeState(SyncStateName.Idle)
	}

	override def doMaintain(): Unit = {
		if (!syncManager.queue.isHashesEmpty) {
			//新たなハッシュ値があるのだから、ダウンロードすべきである。
			syncManager.changeState(SyncStateName.BlockRetrieving)
		} else if (syncManager.queue.isBlocksEmpty && !syncManager.isSyncDone) {
			//まだ同期が完了していないが、キューは空である。
			//ハッシュ値をダウンロードする。
			syncManager.resetGapRecovery()
			syncManager.changeState(SyncStateName.HashRetrieving)
		}
	}
}

class BlockRetrievingState extends AbstractSyncState(SyncStateName.BlockRetrieving) {

	override def doOnTransition(): Unit = {
		syncManager.pool.changeState(SyncStateName.BlockRetrieving)
	}

	override def doMaintain(): Unit = {
		if (syncManager.queue.isHashesEmpty) {
			syncManager.changeState(SyncStateName.Idle)
		} else {
			syncManager.pool.changeState(SyncStateName.BlockRetrieving, (peer: Channel) => peer.isIdle)
		}
	}

}

class HashRetrievingState extends AbstractSyncState(SyncStateName.HashRetrieving) {

	import HashRetrievingState._

	override def doMaintain(): Unit = {
		var master: Channel = null
		val foundOrNone = syncManager.pool.peers.find(peer => peer.isHashRetrievingDone || peer.isHashRetrieving)
		foundOrNone.foreach {
			peer => {
				if (peer.isHashRetrievingDone) {
					//次はブロックを取得する。
					syncManager.changeState(SyncStateName.BlockRetrieving)
					return
				} else {
					master = peer
				}
			}
		}
		if (Option(master).isDefined) {
			if (syncManager.isPeerStuck(master)) {
				logger.info("<SyncState> Banning a peer: %s. Stuck.".format(master.peerIdShort))
				syncManager.pool.ban(master)
				//いちおう、取得できるブロックがないかどうか確認した上で、HashRetrievingを続ける。
				syncManager.changeState(SyncStateName.BlockRetrieving)
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
		syncManager.pool.changeState(SyncStateName.BlockRetrieving, (peer) => peer.isIdle)
	}
}

object HashRetrievingState {
	private val logger = LoggerFactory.getLogger("sync")
}
