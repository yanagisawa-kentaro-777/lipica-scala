package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

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
