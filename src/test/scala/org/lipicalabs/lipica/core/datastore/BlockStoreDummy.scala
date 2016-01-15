package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.kernel.Block
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{Digest256, DigestValue}

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class BlockStoreDummy extends BlockStore {

	override def getBlockHashByNumber(blockNumber: Long): Option[DigestValue] = {
		val data  = String.valueOf(blockNumber).getBytes
		Option(Digest256(DigestUtils.digest256(data)))
	}

	override def getBlockHashByNumber(blockNumber: Long, branchBlockHash: DigestValue): Option[DigestValue] = {
		getBlockHashByNumber(blockNumber)
	}

	override def getChainBlockByNumber(blockNumber: Long) = None

	override def getBlockByHash(hash: DigestValue): Option[Block] = null

	override def existsBlock(hash: DigestValue): Boolean = false

	override def getHashesEndingWith(hash: DigestValue, qty: Long): Seq[DigestValue] = null

	override def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit = ()

	override def getTotalDifficulty: BigInt = null

	override def getBestBlock = None

	override def flush(): Unit = ()

	override def getMaxBlockNumber: Long = 0

	override def rebranch(forkBlock: Block): Unit = ()

	override def getTotalDifficultyForHash(hash: DigestValue): BigInt = null

	override def close(): Unit = ()

}
