package org.lipicalabs.lipica.core.net.channel

import java.util.concurrent.{CopyOnWriteArrayList, Executors, ScheduledExecutorService, TimeUnit}

import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.facade.components.WorldManager
import org.lipicalabs.lipica.core.sync.SyncManager
import org.lipicalabs.lipica.core.utils.CountingThreadFactory
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer

/**
 * 新たに確立されたチャネルについて、
 * 同期処理機構への回送等を行う装置です。
 * 自ノード全体で１個のインスタンスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
class ChannelManager {

	import scala.collection.JavaConversions._

	private def worldManager: WorldManager = WorldManager.instance
	private def syncManager: SyncManager = worldManager.syncManager

	private val newPeers = new CopyOnWriteArrayList[Channel]()
	private val activePeers = new CopyOnWriteArrayList[Channel]()

	private val mainWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("channel-manager"))

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
				//状態同期は完了しているので、トランザクションを処理することができる。
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
