package org.lipicalabs.lipica.core.facade.submit

import java.util.concurrent.Callable

import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:47
 * YANAGISAWA, Kentaro
 */
class TransactionTask(private val tx: TransactionLike, private val componentsMotherboard: ComponentsMotherboard) extends Callable[TransactionLike] {

	import TransactionTask._

	override def call: TransactionLike = {
		try {
			val channelManager = this.componentsMotherboard.channelManager
			logger.info("<TxTask> Submitting tx to %,d peers: %s".format(channelManager.activePeersCount, this.tx))
			channelManager.sendTransaction(this.tx)
			this.tx
		} catch {
			case e: Throwable =>
				ErrorLogger.logger.warn("<TransactionTask> Exception caught: %s".format(e.getClass.getSimpleName), e)
				logger.warn("<TransactionTask> Exception caught: %s".format(e.getClass.getSimpleName), e)
				null
		}
	}
}

object TransactionTask {
	private val logger = LoggerFactory.getLogger("general")
}
