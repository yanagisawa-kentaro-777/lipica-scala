package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.DB
import org.slf4j.LoggerFactory

import scala.collection.mutable


class BlockInfo {
	//TODO
}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/19 20:36
 * YANAGISAWA, Kentaro
 */
class IndexedBlockStore(private val index: mutable.Map[Long, mutable.Buffer[BlockInfo]], private val blocks: KeyValueDataSource, private val cache: IndexedBlockStore, private val indexDB: DB) extends AbstractBlockStore {

	import IndexedBlockStore._

	override def getBestBlock: Option[Block] = {
		val maxLevel = getMaxNumber
		if (maxLevel < 0) {
			return None
		}
		var nextLevel = maxLevel
		while (0 <= nextLevel) {
			getChainBlockByNumber(maxLevel) match {
				case Some(block) => return Some(block)
				case None =>
			}
			nextLevel -= 1
		}
		None
	}

	override def flush() = {
		val startTime = System.nanoTime
		for (key <- this.cache.blocks.keys) {
			this.blocks.put(key, this.cache.blocks.get(key).get)
		}
		for (entry <- this.cache.index) {
			val number = entry._1
			val infoBuffer = entry._2

			if (this.index.contains(number)) {
				infoBuffer.appendAll(this.index.get(number).get)
			}
			this.index.put(number, infoBuffer)
		}
		this.cache.blocks.close()
		this.cache.index.clear()
		val endTime = System.nanoTime

		this.indexDB.commit()
		logger.info("<IndexBlockStore> Flushed block store in %,d nanos.".format(endTime - startTime))
	}

	override def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean) = {
		this.cache.saveBlock(block, cumulativeDifficulty, mainChain)
	}

	override def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes] = getChainBlockByNumber(blockNumber).map(_.hash)

	def getBlocksByNumber(number: Long): Seq[Block] = ???

	override def getChainBlockByNumber(blockNumber: Long): Option[Block] = ???

	override def getBlockByHash(hash: ImmutableBytes): Option[Block] = {
		this.cache.getBlockByHash(hash) match {
			case Some(block) => Some(block)
			case None =>
				this.blocks.get(hash) match {
					case Some(bytes) => Option(Block.decode(bytes))
					case None => None
				}
		}
	}

	override def existsBlock(hash: ImmutableBytes): Boolean = {
		this.cache.getBlockByHash(hash) match {
			case Some(block) => true
			case None => this.blocks.get(hash).isDefined
		}
	}

	override def getTotalDifficultyForHash(hash: ImmutableBytes) = ???

	override def getTotalDifficulty = ???

	override def getMaxNumber: Long = {
		val bestIndex = 0.max(this.index.size).toLong
		bestIndex + this.cache.index.size - 1L
	}

	override def reBranch(forkBlock: Block) = ???

	override def getListHashesEndWith(hash: ImmutableBytes, qty: Long) = ???

	override def load(): Unit = {
		//
	}
}

object IndexedBlockStore {
	private val logger = LoggerFactory.getLogger(getClass)
}
