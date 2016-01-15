package org.lipicalabs.lipica.core.validator.block_rules

import org.lipicalabs.lipica.core.kernel.{Block, BlockHeader}
import org.lipicalabs.lipica.core.utils.{DigestValue, ImmutableBytes}

/**
 * Ommer (=Uncle) に関する制限や仕様です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/28 11:31
 * YANAGISAWA, Kentaro
 */
class UnclesRule extends BlockRule {
	import UnclesRule._

	def validate(block: Block): Boolean = {
		this.errors.clear()
		if (UncleNumberLimit < block.uncles.size) {
			this.errors.append("TOO MANY UNCLES: %d < %d".format(UncleNumberLimit, block.uncles.size))
			return false
		}
		if (block.blockHeader.unclesHash != UnclesRule.calculateUnclesHash(block.uncles)) {
			this.errors.append("BAD UNCLE HASH: %s != %s".format(block.blockHeader.unclesHash, calculateUnclesHash(block.uncles)))
			return false
		}
		true
	}
}

object UnclesRule {

	/**
	 * １ブロック内のUncle上限は２．
	 */
	val UncleNumberLimit = 2

	/**
	 * Uncleは７代遡って認める。
	 * （実データに存在する。）
	 */
	val UncleGenerationLimit = 7

	/**
	 * Uncleのハッシュ値を計算するための中心アルゴリズム！
	 */
	def calculateUnclesHash(uncles: Seq[BlockHeader]): DigestValue = {
		BlockHeader.encodeUncles(uncles).digest256
	}

}
