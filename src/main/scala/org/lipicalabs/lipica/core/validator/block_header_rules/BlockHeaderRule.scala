package org.lipicalabs.lipica.core.validator.block_header_rules

import org.lipicalabs.lipica.core.kernel.BlockHeader
import org.lipicalabs.lipica.core.validator.AbstractValidationRule

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:06
 * YANAGISAWA, Kentaro
 */
trait BlockHeaderRule extends AbstractValidationRule {

	override def getEntityClass = classOf[BlockHeader]

	def validate(header: BlockHeader): Boolean

}
