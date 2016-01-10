package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.kernel.BlockHeader

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:06
 * YANAGISAWA, Kentaro
 */
abstract class BlockHeaderRule extends AbstractValidationRule {

	override def getEntityClass = BlockHeader.getClass

	def validate(header: BlockHeader): Boolean

}
