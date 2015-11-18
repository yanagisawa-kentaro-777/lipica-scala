package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.BlockHeader

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:07
 * YANAGISAWA, Kentaro
 */
class ProofOfWorkRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		this.errors.clear()

		val proof = header.calculateProofOfWorkValue
		val boundary = header.getProofOfWorkBoundary

		(proof compareTo boundary) <= 0
	}

}
