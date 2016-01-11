package org.lipicalabs.lipica.core.validator.parent_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader
import org.lipicalabs.lipica.core.validator.AbstractValidationRule

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:46
 * YANAGISAWA, Kentaro
 */
trait DependentBlockHeaderRule extends AbstractValidationRule {

	override def getEntityClass = classOf[BlockHeader]

	def validate(header: BlockHeader, dependency: BlockHeader): Boolean
}
