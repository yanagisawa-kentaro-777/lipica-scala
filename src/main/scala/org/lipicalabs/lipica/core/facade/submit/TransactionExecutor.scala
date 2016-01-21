package org.lipicalabs.lipica.core.facade.submit

import java.util.concurrent.Future

import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.kernel.TransactionLike

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:45
 * YANAGISAWA, Kentaro
 */
object TransactionExecutor {
	def submitTransaction(task: TransactionTask): Future[TransactionLike] = ExecutorPool.instance.txExecutor.submit(task)
}
