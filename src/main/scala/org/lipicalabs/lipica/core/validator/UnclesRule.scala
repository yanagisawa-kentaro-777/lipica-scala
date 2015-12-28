package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.{Block, BlockHeader}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

import scala.collection.mutable.ArrayBuffer

/**
 * Ommer (=Uncle) に関する制限や仕様です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/28 11:31
 * YANAGISAWA, Kentaro
 */
class UnclesRule {
	import UnclesRule._

	private val _errors = new ArrayBuffer[String]

	def errors: Seq[String] = this._errors.toSeq

	def validate(block: Block): Boolean = {
		_errors.clear()
		if (UncleNumberLimit < block.uncles.size) {
			_errors.append("TOO MANY UNCLES: %d < %d".format(UncleNumberLimit, block.uncles.size))
			return false
		}
		if (block.blockHeader.unclesHash != UnclesRule.calculateUnclesHash(block.uncles)) {
			_errors.append("BAD UNCLE HASH: %s != %s".format(block.blockHeader.unclesHash, calculateUnclesHash(block.uncles)))
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
	def calculateUnclesHash(uncles: Seq[BlockHeader]): ImmutableBytes = {
		BlockHeader.encodeUncles(uncles).digest256
	}

}
