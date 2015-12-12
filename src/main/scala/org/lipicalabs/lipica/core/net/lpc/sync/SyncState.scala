package org.lipicalabs.lipica.core.net.lpc.sync

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