package org.lipicalabs.lipica.core.utils

import org.lipicalabs.lipica.core.config.NodeProperties

/**
 * Created by IntelliJ IDEA.
 * 2015/11/07 14:06
 * YANAGISAWA, Kentaro
 */
object UtilConsts {

	val Zero: BigInt = BigInt(0)
	val One: BigInt = BigInt(1)

	val MinimumDifficulty = BigInt(131072)
	val DifficultyBoundDivisor: BigInt = BigInt(2048)
	val DurationLimit = if (NodeProperties.instance.isFrontier) 13 else 8
	val ExpDifficultyPeriod: Int = 100000
}
