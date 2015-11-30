package org.lipicalabs.lipica.core.net.submit

import java.util.concurrent.{Future, Executors}

import org.lipicalabs.lipica.core.base.TransactionLike

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:45
 * YANAGISAWA, Kentaro
 */
object TransactionExecutor {
	private val executor = Executors.newFixedThreadPool(1)

	def submitTransaction(task: TransactionTask): Future[TransactionLike] = this.executor.submit(task)
}
