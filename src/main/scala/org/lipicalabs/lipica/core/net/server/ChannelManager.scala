package org.lipicalabs.lipica.core.net.server

import java.util.concurrent.{CopyOnWriteArrayList, TimeUnit, Executors, ScheduledExecutorService}

import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.lpc.sync.SyncManager
import org.lipicalabs.lipica.core.net.secure.discover.NodeManager
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
class ChannelManager {

	import ChannelManager._
	import scala.collection.JavaConversions._

	//TODO auto wiring
	private val worldManager: WorldManager = null
	private val syncManager: SyncManager = null
	private val nodeManager: NodeManager = null
	private val lipica: Lipica = null

	private val newPeers = asScalaBuffer(new CopyOnWriteArrayList[Channel]())
	private val activePeers = asScalaBuffer(new CopyOnWriteArrayList[Channel]())

	private val mainWorker: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor

	def init(): Unit = {
		this.mainWorker.scheduleWithFixedDelay(new Runnable {
			override def run(): Unit = {
				processNewPeers()
			}
		}, 0L, 1L, TimeUnit.SECONDS)
	}

	private def processNewPeers(): Unit = {
		for (peer <- this.newPeers) {
			//TODO 未実装。

		}
	}

}

object ChannelManager {
	private val logger = LoggerFactory.getLogger("net")
}
