package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:48
 * YANAGISAWA, Kentaro
 */
class ParentBlockHeaderValidator(private val rules: Iterable[DependentBlockHeaderRule]) extends DependentBlockHeaderRule {

	override def validate(header: BlockHeader, parent: BlockHeader): Boolean = {
		this.errors.clear()
		for (rule <- this.rules) {
			if (!rule.validate(header, parent)) {
				this.errors.appendAll(rule.getErrors)
				return false
			}
		}
		true
	}

}
