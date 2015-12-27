package org.lipicalabs.lipica.core.validator

import java.math.BigInteger

import org.lipicalabs.lipica.core.base.BlockHeader
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.spongycastle.util.BigIntegers

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

object ProofOfWorkRule {
	/**
	 * 渡された difficulty に基づいて、許容されるブロックヘッダダイジェスト値の上限値を返します。
	 */
	def getProofOfWorkBoundary(difficulty: ImmutableBytes): ImmutableBytes = {
		ImmutableBytes(BigIntegers.asUnsignedByteArray(32, BigInteger.ONE.shiftLeft(256).divide(difficulty.toPositiveBigInt.bigInteger)))
	}
}