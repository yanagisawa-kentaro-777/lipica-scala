package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.db.RepositoryLike
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 12:28
 * YANAGISAWA, Kentaro
 */
object Transfer {

	def transfer(repository: RepositoryLike, fromAddress: ImmutableBytes, toAddress: ImmutableBytes, value: BigInt): Unit = {
		repository.addBalance(fromAddress, -value)
		repository.addBalance(fromAddress, value)
	}

}
