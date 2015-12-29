package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ManaValueRule extends BlockHeaderRule {
	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (header.manaLimit.toPositiveBigInt < BigInt(header.manaUsed)) {
			println("ManaUsed %,d < %,d".format(header.manaLimit.toPositiveBigInt, header.manaUsed))//TODO 20151229 DEBUG
			errors.append("ManaUsed %,d < %,d".format(header.manaLimit.toPositiveBigInt, header.manaUsed))
			false
		} else {
			true
		}
	}
	
}
