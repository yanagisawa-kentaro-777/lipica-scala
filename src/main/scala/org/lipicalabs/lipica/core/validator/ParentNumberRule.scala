package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ParentNumberRule extends DependentBlockHeaderRule {

	override def validate(header: BlockHeader, parent: BlockHeader): Boolean = {
		errors.clear()
		if (header.blockNumber != (parent.blockNumber + 1)) {
			errors.append("Block number error (%,d + 1) != %,d".format(parent.blockNumber, header.blockNumber))
			false
		} else {
			true
		}
	}

}
