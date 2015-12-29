package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 *
 * @since 2015/11/29
 * @author YANAGISAWA, Kentaro
 */
class ExtraDataRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		errors.clear()
		if (ExtraDataRule.MaxExtraDataSize < header.extraData.length) {
			println("ExtraDataSize %,d < %,d".format(ExtraDataRule.MaxExtraDataSize, header.extraData.length))//TODO 20151229 DEBUG
			errors.append("ExtraDataSize %,d < %,d".format(ExtraDataRule.MaxExtraDataSize, header.extraData.length))
			false
		} else {
			true
		}
	}

}

object ExtraDataRule {
	val MaxExtraDataSize = 32
}
