package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
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

	def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes]

	def getChainBlockByNumber(blockNumber: Long): Option[Block]

	def getBlockByHash(hash: ImmutableBytes): Option[Block]

	def existsBlock(hash: ImmutableBytes): Boolean

	def getListHashesEndWith(hash: ImmutableBytes, qty: Long): Seq[ImmutableBytes]

	def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit

	def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt

	def getTotalDifficulty: BigInt

	def getBestBlock: Option[Block]

	def getMaxNumber: Long

	def flush(): Unit

	def rebranch(forkBlock: Block): Unit

	def load(): Unit

}
