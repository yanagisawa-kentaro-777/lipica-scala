package org.lipicalabs.lipica.core.validator.block_rules

import org.lipicalabs.lipica.core.kernel.Block
import org.lipicalabs.lipica.core.validator.AbstractValidationRule

/**
 * Created by IntelliJ IDEA.
 * 2016/01/11 13:31
 * YANAGISAWA, Kentaro
 */
trait BlockRule extends AbstractValidationRule {

	override def getEntityClass = classOf[Block]

	def validate(block: Block): Boolean

}