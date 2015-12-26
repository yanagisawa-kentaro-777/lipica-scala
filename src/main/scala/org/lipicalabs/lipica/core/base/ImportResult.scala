package org.lipicalabs.lipica.core.base

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 11:09
 * YANAGISAWA, Kentaro
 */
sealed trait ImportResult {
	def isSuccessful: Boolean
}

object ImportResult {
	case object ImportedBest extends ImportResult {
		override val isSuccessful: Boolean = true
	}
	case object ImportedNotBest extends ImportResult {
		override val isSuccessful: Boolean = true
	}
	case object Exists extends ImportResult {
		override val isSuccessful: Boolean = false
	}
	case object NoParent extends ImportResult {
		override val isSuccessful: Boolean = false
	}
	case object ConsensusBreak extends ImportResult {
		override val isSuccessful: Boolean = false
	}

}