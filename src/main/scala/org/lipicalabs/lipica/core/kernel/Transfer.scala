package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.db.RepositoryLike
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 12:28
 * YANAGISAWA, Kentaro
 */
object Transfer {

	private val logger = LoggerFactory.getLogger("execute")

	def transfer(repository: RepositoryLike, fromAddress: ImmutableBytes, toAddress: ImmutableBytes, value: BigInt): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<Transfer> Transferring %,d from %s to %s".format(value, fromAddress, toAddress))
		}
		repository.addBalance(fromAddress, -value)
		repository.addBalance(toAddress, value)
	}

}
