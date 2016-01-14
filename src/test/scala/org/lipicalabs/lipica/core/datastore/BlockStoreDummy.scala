package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.kernel.Block
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class BlockStoreDummy extends BlockStore {

	override def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes] = {
		val data  = String.valueOf(blockNumber).getBytes
		Option(ImmutableBytes(DigestUtils.digest256(data)))
	}

	override def getBlockHashByNumber(blockNumber: Long, branchBlockHash: ImmutableBytes): Option[ImmutableBytes] = {
		getBlockHashByNumber(blockNumber)
	}

	override def getChainBlockByNumber(blockNumber: Long) = None

	override def getBlockByHash(hash: ImmutableBytes): Option[Block] = null

	override def existsBlock(hash: ImmutableBytes): Boolean = false

	override def getHashesEndingWith(hash: ImmutableBytes, qty: Long): Seq[ImmutableBytes] = null

	override def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit = ()

	override def getTotalDifficulty: BigInt = null

	override def getBestBlock = None

	override def flush(): Unit = ()

	override def getMaxBlockNumber: Long = 0

	override def rebranch(forkBlock: Block): Unit = ()

	override def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt = null

	override def close(): Unit = ()

}
