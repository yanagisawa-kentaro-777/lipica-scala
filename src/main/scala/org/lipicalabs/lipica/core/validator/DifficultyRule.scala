package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.kernel.BlockHeader
import org.lipicalabs.lipica.core.utils.UtilConsts._

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class DifficultyRule extends DependentBlockHeaderRule {
	override def validate(header: BlockHeader, parent: BlockHeader): Boolean = {
		errors.clear()
		val calculated = header.calculateDifficulty(parent)
		val actual = header.difficultyAsBigInt

		if (actual != calculated) {
			errors.append("Difficulty %,d != %,d".format(calculated, actual))
			false
		} else {
			true
		}
	}
}

object DifficultyRule {

	/**
	 * difficulty 遷移の中心アルゴリズム！
	 */
	def calculateDifficulty(parent: BlockHeader, newBlockNumber: Long, newTimeStamp: Long): BigInt = {
		val parentDifficulty = parent.difficultyAsBigInt
		val quotient = parentDifficulty / DifficultyBoundDivisor

		val fromParent = if ((parent.timestamp + DurationLimit) <= newTimeStamp) {
			parentDifficulty - quotient
		} else {
			parentDifficulty + quotient
		}

		val periodCount = (newBlockNumber / ExpDifficultyPeriod).toInt
		val difficulty = MinimumDifficulty max fromParent
		if (1 < periodCount) {
			MinimumDifficulty max (difficulty + (One << (periodCount - 2)))
		} else {
			difficulty
		}
	}
}

