package org.lipicalabs.lipica.core.validator.block_header_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ExtraDataRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (ExtraDataRule.MaxExtraDataSize < header.extraData.length) {
			errors.append("[Block %,d] ExtraDataSize %,d < %,d".format(header.blockNumber, ExtraDataRule.MaxExtraDataSize, header.extraData.length))
			false
		} else {
			true
		}
	}

}

object ExtraDataRule {
	val MaxExtraDataSize = 32
}
