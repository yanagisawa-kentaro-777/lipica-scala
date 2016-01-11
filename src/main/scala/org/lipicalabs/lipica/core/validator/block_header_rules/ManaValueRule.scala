package org.lipicalabs.lipica.core.validator.block_header_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ManaValueRule extends BlockHeaderRule {
	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (header.manaLimit.toPositiveBigInt < BigInt(header.manaUsed)) {
			errors.append("[Block %,d] ManaUsed %,d < %,d".format(header.blockNumber, header.manaLimit.toPositiveBigInt, header.manaUsed))
			false
		} else {
			true
		}
	}
	
}
