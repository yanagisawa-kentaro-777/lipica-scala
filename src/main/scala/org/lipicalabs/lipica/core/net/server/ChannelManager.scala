package org.lipicalabs.lipica.core.net.server

import java.util.concurrent.{CopyOnWriteArrayList, TimeUnit, Executors, ScheduledExecutorService}

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.lpc.sync.SyncManager
import org.lipicalabs.lipica.core.net.transport.discover.NodeManager
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
class ChannelManager {

	import ChannelManager._
	import scala.collection.JavaConversions._

	//TODO auto wiring
	private val worldManager: WorldManager = ???
	private val syncManager: SyncManager = ???
	private val nodeManager: NodeManager = ???
	private val lipica: Lipica = ???

	private val newPeers = new CopyOnWriteArrayList[Channel]()
	private val activePeers = new CopyOnWriteArrayList[Channel]()

	private val mainWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor

	def init(): Unit = {
		this.mainWorker.scheduleWithFixedDelay(new Runnable {
			override def run(): Unit = {
				processNewPeers()
			}
		}, 0L, 1L, TimeUnit.SECONDS)
	}

	private def processNewPeers(): Unit = {
		val processed = new ArrayBuffer[Channel]
		for (peer <- this.newPeers) {
			if (peer.isProtocolInitialized) {
				process(peer)
				processed.append(peer)
			}
		}
		this.newPeers.removeAll(processed)
	}

	private def process(peer: Channel): Unit = {
		if (peer.hasLpcStatusSucceeded) {
			if (this.syncManager.isSyncDone) {
				peer.onSyncDone()
			}
			this.syncManager.addPeer(peer)
			this.activePeers.add(peer)
		}
	}

	def sendTransaction(tx: TransactionLike): Unit = {
		this.activePeers.foreach(channel => channel.sendTransaction(tx))
	}

	def add(channel: Channel): Unit = this.newPeers.add(channel)

	def notifyDisconnect(channel: Channel): Unit = {
		channel.onDisconnect()
		this.syncManager.onDisconnect(channel)
		this.activePeers.remove(channel)
		this.newPeers.remove(channel)
	}

	def onSyncDone(): Unit = {
		this.activePeers.foreach(_.onSyncDone())
	}

}

object ChannelManager {
	private val logger = LoggerFactory.getLogger("net")
}
