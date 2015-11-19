package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class BlockStoreDummy extends BlockStore {

	override def getBlockHashByNumber(blockNumber: Long): ImmutableBytes = {
		val data  = String.valueOf(blockNumber).getBytes
		ImmutableBytes(DigestUtils.keccak256(data))
	}

	override def getBlockHashByNumber(blockNumber: Long, branchBlockHash: ImmutableBytes): ImmutableBytes = {
		getBlockHashByNumber(blockNumber)
	}

	override def getChainBlockByNumber(blockNumber: Long): Block = {
		null
	}

	override def getBlockByHash(hash: ImmutableBytes): Block = {
		null
	}

	override def isBlockExist(hash: ImmutableBytes): Boolean = {
		false
	}

	override def getListHashesEndWith(hash: ImmutableBytes, qty: Long): Seq[ImmutableBytes] = {
		null
	}

	override def saveBlock(block: Block, cummDifficulty: BigInt, mainChain: Boolean) {
	}

	override def getTotalDifficulty: BigInt = {
		null
	}

	override def getBestBlock: Block = {
		null
	}

	override def flush(): Unit = {
		//
	}

	override def load(): Unit = {
		//
	}

	override def getMaxNumber: Long = {
		0
	}

	override def reBranch(forkBlock: Block): Unit = {
		//
	}

	override def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt = {
		null
	}

}
