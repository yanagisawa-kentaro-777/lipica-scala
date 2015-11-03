package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:08
 * YANAGISAWA, Kentaro
 */
trait BlockStore {
	//TODO

	/**
	 * ブロック番号で、ブロックハッシュを引いて返します。
	 * フォークが発生している場合には、渡された枝の祖先に当たるものを返します。
	 */
	def getBlockHashByNumber(blockNumber: Long, branchBlockHash: ImmutableBytes): ImmutableBytes

	def getBlockHashByNumber(blockNumber: Long): ImmutableBytes

	def getChainBlockByNumber(blockNumber: Long): Block

	def getBlockByHash(hash: ImmutableBytes): Block

	def isBlockExist(hash: ImmutableBytes): Boolean

	def getListHashesEndWith(hash: ImmutableBytes, qty: Long): Seq[ImmutableBytes]

	def saveBlock(block: Block, cummDifficulty: BigInt, mainChain: Boolean)

	def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt

	def getTotalDifficulty: BigInt

	def getBestBlock: Block

	def getMaxNumber: Long

	def flush()

	def reBranch(forkBlock: Block)

	def load()

	//TODO Hibernateには依存できない！
	//def setSessionFactory(sessionFactory: SessionFactory)
}
