package org.lipicalabs.lipica.core.validator.block_header_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ManaLimitRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (header.manaLimit.positiveBigInt < BigInt(ManaLimitRule.MinManaLimit)) {
			errors.append("ManaLimit %,d < %,d".format(header.manaLimit.positiveBigInt, ManaLimitRule.MinManaLimit))
			false
		} else {
			true
		}
	}

}

object ManaLimitRule {
	val MinManaLimit = 125000
}