package org.lipicalabs.lipica.core.net.lpc.sync

import java.util.concurrent.atomic.AtomicReference

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
 * 同期状態を表現する trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:11
 * YANAGISAWA, Kentaro
 */
sealed trait SyncState {

	/**
	 * この状態の「名前」すなわち識別子を返します。
	 */
	def name: SyncStateName

	/**
	 * この状態が、渡された「名前」のものであるか否かを返します。
	 */
	def is(name: SyncStateName): Boolean

	/**
	 * この状態に遷移する直前に実行される処理です。
	 */
	def doOnTransition(): Unit

	/**
	 * この状態に留まっている間、定期的に実行される処理です。
	 */
	def doMaintain(): Unit

	/**
	 * 状態の遷移や維持に伴うコールバック先として、
	 * 渡された SyncManager を記憶します。
	 */
	def assign(syncManager: SyncManager): Unit

}

abstract class AbstractSyncState(override val name: SyncStateName) extends SyncState {

	private val syncManagerRef: AtomicReference[SyncManager] = new AtomicReference[SyncManager](null)
	override def assign(v: SyncManager): Unit = this.syncManagerRef.set(v)
	protected def syncManager: SyncManager = this.syncManagerRef.get

	override def is(aName: SyncStateName): Boolean = this.name == aName
	override def doOnTransition(): Unit = {
		//
	}
	override def doMaintain(): Unit = {
		//
	}
	override def toString: String = this.name.toString
}

/**
 * 何もしていない状態を表すクラスです。
 */
class IdleState extends AbstractSyncState(SyncStateName.Idle) {

	override def doOnTransition(): Unit = {
		//すべてのアクティブなピアの状態を Idle にする。
		syncManager.pool.changeState(SyncStateName.Idle)
	}

	override def doMaintain(): Unit = {
		if (!syncManager.queue.isHashesEmpty) {
			//新たなハッシュ値があるのだから、ダウンロードすべきである。
			syncManager.changeState(SyncStateName.BlockRetrieving)
		} else if (syncManager.queue.isBlocksEmpty && !syncManager.isSyncDone) {
			//まだ同期が完了していないが、キューのブロックは空である。
			//ハッシュ値をダウンロードする。
			syncManager.resetGapRecovery()
			syncManager.changeState(SyncStateName.HashRetrieving)
		}
	}
}

/**
 * ブロックをダウンロードしようとしている状態を表すクラスです。
 */
class BlockRetrievingState extends AbstractSyncState(SyncStateName.BlockRetrieving) {

	override def doOnTransition(): Unit = {
		//すべてのアクティブなピアの状態を同調させる。
		syncManager.pool.changeState(SyncStateName.BlockRetrieving)
	}

	override def doMaintain(): Unit = {
		if (syncManager.queue.isHashesEmpty) {
			//ダウンロードすべきブロックが今はないので、休む。
			syncManager.changeState(SyncStateName.Idle)
		} else {
			//暇なピアに、ブロックをダウンロードするよう督励する。
			syncManager.pool.changeState(SyncStateName.BlockRetrieving, (peer: Channel) => peer.isIdle)
		}
	}
}

/**
 * ブロックのダイジェスト値を収集しようとしている状態を表すクラスです。
 */
class HashRetrievingState extends AbstractSyncState(SyncStateName.HashRetrieving) {

	import HashRetrievingState._

	override def doMaintain(): Unit = {
		var master: Channel = null
		//実際にダイジェスト値を収集していたピアは１個だけのはずなので、それを見つけようとする。
		val foundOrNone = syncManager.pool.peers.find(peer => peer.isHashRetrievingDone || peer.isHashRetrieving)
		foundOrNone.foreach {
			peer => {
				if (peer.isHashRetrievingDone) {
					//ハッシュ値の収集は完了していたらしい。次はブロックを取得する。
					syncManager.changeState(SyncStateName.BlockRetrieving)
					return
				} else {
					//これがマスターだったわけだ。
					master = peer
				}
			}
		}
		if (Option(master).isDefined) {
			//前回のマスターが見つかった。
			if (syncManager.isPeerStuck(master)) {
				//マスターに問題があったらしい。
				logger.info("<SyncState> Banning a peer: %s. Stuck.".format(master.peerIdShort))
				syncManager.pool.ban(master, Stuck)
				//いちおう、現状で取得できるブロックがないかどうか確認した上で、状態に応じて HashRetrieving に戻る。
				syncManager.changeState(SyncStateName.BlockRetrieving)
				return
			}
			//既存の問題ないなら、そのまま継続。
		} else {
			if (logger.isTraceEnabled) {
				logger.trace("<HashRetrievingState> Hash retrieving is in progress.")
			}
			//マスターが選定されていないか行方不明になってしまったので、 以下で新たにマスターピアを選択する。
			//
			//ギャップを解決中であるならば、そのギャップの発生源ノードをマスターにする。
			if (syncManager.gapBlockOption.isDefined) {
				master = syncManager.pool.getByNodeId(syncManager.gapBlockOption.get.nodeId).orNull
			}
			//もっともTDが大きいピア。
			if (master eq null) {
				master = syncManager.pool.getBest.orNull
			}
			if (master eq null) {
				//候補がいないからどうしようもない。次のチャンスに期待する。
				return
			}
			//マスターを切り替える。
			syncManager.startMaster(master)
		}
		//暇なピアには、ブロック収集をさせておく。
		syncManager.pool.changeState(SyncStateName.BlockRetrieving, (peer) => peer.isIdle)
	}
}

object HashRetrievingState {
	private val logger = LoggerFactory.getLogger("sync")
}
