package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ブロックの保存および取得を行うための
 * コンポーネントが実装すべきインターフェイスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:08
 * YANAGISAWA, Kentaro
 */
trait BlockStore {

	/**
	 * ブロック番号で、ブロックハッシュを引いて返します。
	 * フォークが発生している場合には、渡された枝の祖先に当たるものを返します。
	 */
	def getBlockHashByNumber(blockNumber: Long, branchBlockHash: ImmutableBytes): Option[ImmutableBytes]

	/**
	 * ブロック番号で、ブロックハッシュを引いて返します。
	 * （メインチェーンのみ）
	 */
	def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes]

	/**
	 * ブロック番号で、ブロックを引いて返します。
	 * （メインチェーンのみ）
	 */
	def getChainBlockByNumber(blockNumber: Long): Option[Block]

	/**
	 * ハッシュ値からブロックを引いて返します。（メインチェーンのみ）
	 */
	def getBlockByHash(hash: ImmutableBytes): Option[Block]

	/**
	 * 指定されたハッシュ値を持つブロックが存在するか否かを返します。
	 */
	def existsBlock(hash: ImmutableBytes): Boolean

	/**
	 * 指定されたハッシュ値で終わるハッシュ値の並びを、指定された個数分返します。
	 */
	def getHashesEndingWith(hash: ImmutableBytes, count: Long): Seq[ImmutableBytes]

	/**
	 * ブロックを保存します。
	 */
	def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit

	/**
	 * 指定されたハッシュ値を持つブロックにおける難度を返します。
	 */
	def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt

	/**
	 * 最新ブロック時点の難度を返します。
	 */
	def getTotalDifficulty: BigInt

	/**
	 * 最新ブロックを返します。
	 */
	def getBestBlock: Option[Block]

	/**
	 * 最新ブロック番号を返します。
	 */
	def getMaxBlockNumber: Long

	/**
	 * 永続化します。
	 */
	def flush(): Unit

	/**
	 * メインとフォークとを切り換えます。
	 */
	def rebranch(forkBlock: Block): Unit

	def load(): Unit

}
