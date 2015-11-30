package org.lipicalabs.lipica.core.net.submit

import java.util.concurrent.Callable

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.manager.WorldManager
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:47
 * YANAGISAWA, Kentaro
 */
class TransactionTask(private val tx: TransactionLike, private val worldManager: WorldManager) extends Callable[TransactionLike] {

	import TransactionTask._

	override def call: TransactionLike = {
		try {
			//TODO 未実装。ChannelManager
			this.tx
		} catch {
			case e: Throwable =>
				logger.warn("<TransactionTask>", e)
				null
		}
	}
}

object TransactionTask {
	private val logger = LoggerFactory.getLogger("net")
}
