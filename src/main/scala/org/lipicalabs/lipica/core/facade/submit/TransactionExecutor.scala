package org.lipicalabs.lipica.core.facade.submit

import java.util.concurrent.{Future, Executors}

import org.lipicalabs.lipica.core.concurrent.CountingThreadFactory
import org.lipicalabs.lipica.core.kernel.TransactionLike

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:45
 * YANAGISAWA, Kentaro
 */
object TransactionExecutor {
	private val executor = Executors.newFixedThreadPool(1, new CountingThreadFactory("tx-executor"))

	def submitTransaction(task: TransactionTask): Future[TransactionLike] = this.executor.submit(task)
}
