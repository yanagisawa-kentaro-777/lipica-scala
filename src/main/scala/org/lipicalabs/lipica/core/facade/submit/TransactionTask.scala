package org.lipicalabs.lipica.core.facade.submit

import java.util.concurrent.Callable

import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * 外部アプリケーション等フロントエンドから、トランザクションを実行する処理者クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:47
 * YANAGISAWA, Kentaro
 */
class TransactionTask(private val tx: TransactionLike, private val componentsMotherboard: ComponentsMotherboard) extends Callable[TransactionLike] {

	import TransactionTask._

	override def call: TransactionLike = {
		val channelManager = this.componentsMotherboard.channelManager
		logger.info("<TxTask> Submitting tx to %,d peers: %s".format(channelManager.activePeersCount, this.tx))
		channelManager.sendTransaction(this.tx)
		this.tx
	}
}

object TransactionTask {
	private val logger = LoggerFactory.getLogger("general")
}
