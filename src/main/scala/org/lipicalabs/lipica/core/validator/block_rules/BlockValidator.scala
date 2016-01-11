package org.lipicalabs.lipica.core.validator.block_rules

import org.lipicalabs.lipica.core.kernel.Block

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:48
 * YANAGISAWA, Kentaro
 */
class BlockValidator(private val rules: Iterable[BlockRule]) extends BlockRule {

	override def validate(block: Block): Boolean = {
		this.errors.clear()
		for (rule <- this.rules) {
			if (!rule.validate(block)) {
				this.errors.appendAll(rule.getErrors)
				return false
			}
		}
		true
	}

}
