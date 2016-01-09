package org.lipicalabs.lipica.core.base

/**
 * 自ノードのブロックチェーンにブロックを連結しようと試行した結果を表す trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/22 11:09
 * YANAGISAWA, Kentaro
 */
sealed trait ImportResult {
	def isSuccessful: Boolean
}

object ImportResult {

	/**
	 * 最先端に連結できた。
	 */
	case object ImportedBest extends ImportResult {
		override val isSuccessful: Boolean = true
	}

	/**
	 * フォークとして連結できた。
	 */
	case object ImportedNotBest extends ImportResult {
		override val isSuccessful: Boolean = true
	}

	/**
	 * このブロックはすでに存在した。
	 */
	case object Exists extends ImportResult {
		override val isSuccessful: Boolean = false
	}

	/**
	 * このブロックを連結すべき場所がない。
	 */
	case object NoParent extends ImportResult {
		override val isSuccessful: Boolean = false
	}

	/**
	 * このブロックを連結すると、状態が合わなくなる。
	 */
	case object ConsensusBreak extends ImportResult {
		override val isSuccessful: Boolean = false
	}

	/**
	 * このブロックは毀れている。
	 */
	case object InvalidBlock extends ImportResult {
		override val isSuccessful: Boolean = false
	}

}