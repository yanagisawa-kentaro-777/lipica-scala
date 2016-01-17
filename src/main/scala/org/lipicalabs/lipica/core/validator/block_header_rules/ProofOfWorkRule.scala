package org.lipicalabs.lipica.core.validator.block_header_rules

import java.math.BigInteger

import org.lipicalabs.lipica.core.kernel.BlockHeader
import org.lipicalabs.lipica.core.utils.{UtilConsts, BigIntBytes, ImmutableBytes}
import org.spongycastle.util.BigIntegers

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 22:07
 * YANAGISAWA, Kentaro
 */
class ProofOfWorkRule extends BlockHeaderRule {

	override def validate(header: BlockHeader): Boolean = {
		this.errors.clear()

		val proof = header.calculateProofOfWorkValue.positiveBigInt
		val boundary = header.getProofOfWorkBoundary

		if (proof <= boundary) {
			true
		} else {
			this.errors.append("[Block %,d] Bad PoW. %s < %s".format(header.blockNumber, boundary, proof))
			//println("[Block %,d] Bad PoW. %s < %s".format(header.blockNumber, boundary, proof))
			false
		}
	}

}

object ProofOfWorkRule {
	/**
	 * 渡された difficulty に基づいて、許容されるブロックヘッダダイジェスト値の上限値を返します。
	 */
	def calculateProofOfWorkBoundary(difficulty: BigIntBytes): BigInt = {
		(UtilConsts.One << 256) / difficulty.positiveBigInt
	}
}