package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:46
 * YANAGISAWA, Kentaro
 */
abstract class DependentBlockHeaderRule extends AbstractValidationRule {

	override def getEntityClass = BlockHeader.getClass

	def validate(header: BlockHeader, dependency: BlockHeader): Boolean
}
