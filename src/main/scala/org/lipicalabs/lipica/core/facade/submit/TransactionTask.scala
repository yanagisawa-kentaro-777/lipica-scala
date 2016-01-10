package org.lipicalabs.lipica.core.facade.submit

import java.util.concurrent.Callable

import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.facade.manager.WorldManager
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
			logger.info("<TxTask> Submitting tx: %s".format(this.tx))
			this.worldManager.channelManager.sendTransaction(this.tx)
			this.tx
		} catch {
			case e: Throwable =>
				logger.warn("<TransactionTask> Exception caught: %s".format(e.getClass.getSimpleName), e)
				null
		}
	}
}

object TransactionTask {
	private val logger = LoggerFactory.getLogger("net")
}
