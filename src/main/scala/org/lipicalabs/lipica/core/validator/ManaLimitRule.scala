package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ManaLimitRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (header.manaLimit < ManaLimitRule.MinManaLimit) {
			errors.append("ManaLimit %,d < %,d".format(header.manaLimit, ManaLimitRule.MinManaLimit))
			false
		} else {
			true
		}
	}

}

object ManaLimitRule {
	val MinManaLimit = 125000
}