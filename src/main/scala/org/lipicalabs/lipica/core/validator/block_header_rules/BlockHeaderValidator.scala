package org.lipicalabs.lipica.core.validator.block_header_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:48
 * YANAGISAWA, Kentaro
 */
class BlockHeaderValidator(private val rules: Iterable[BlockHeaderRule]) extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		this.errors.clear()
		for (rule <- this.rules) {
			if (!rule.validate(header)) {
				this.errors.appendAll(rule.getErrors)
				return false
			}
		}
		true
	}

}
