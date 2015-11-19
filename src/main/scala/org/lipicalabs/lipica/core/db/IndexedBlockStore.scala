package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.mapdb.DB
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


class BlockInfo {
	//TODO case class にできそう。

	private var _hash: ImmutableBytes = ImmutableBytes.empty
	def hash: ImmutableBytes = this._hash
	def hash_=(v: ImmutableBytes): Unit = this._hash = v

	private var _cumulativeDifficulty: BigInt = UtilConsts.Zero
	def cumulativeDifficulty: BigInt = this._cumulativeDifficulty
	def cumulativeDifficulty_=(v: BigInt): Unit = this._cumulativeDifficulty = v

	private var _mainChain: Boolean = false
	def mainChain: Boolean = this._mainChain
	def mainChain_=(v: Boolean): Unit = this._mainChain = v
}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/19 20:36
 * YANAGISAWA, Kentaro
 */
class IndexedBlockStore(private val index: mutable.Map[Long, mutable.Buffer[BlockInfo]], private val blocks: KeyValueDataSource, private val cache: IndexedBlockStore, private val indexDB: DB) extends AbstractBlockStore {
	//TODO Bufferではなくせそう。

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

	override def flush(): Unit = {
		if (this.cache eq null) {
			return
		}

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
		if (this.cache == null) {
			addInternalBlock(block, cumulativeDifficulty, mainChain)
		} else {
			this.cache.saveBlock(block, cumulativeDifficulty, mainChain)
		}
	}

	private def addInternalBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit = {
		val blockInfoBuffer = this.index.getOrElse(block.blockNumber, new ArrayBuffer[BlockInfo])

		val blockInfo = new BlockInfo
		blockInfo.cumulativeDifficulty = cumulativeDifficulty
		blockInfo.hash = block.hash
		blockInfo.mainChain = mainChain

		blockInfoBuffer.append(blockInfo)

		this.index.put(block.blockNumber, blockInfoBuffer)
		this.blocks.put(block.hash, block.encode)
	}

	override def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes] = getChainBlockByNumber(blockNumber).map(_.hash)

	def getBlocksByNumber(number: Long): Seq[Block] = {
		val result =
			if (this.cache ne null) {
				this.cache.getBlocksByNumber(number).toBuffer
			} else {
				new ArrayBuffer[Block]
			}
		this.index.get(number) match {
			case Some(blockInfoBuffer) =>
				for (blockInfo <- blockInfoBuffer) {
					val hash = blockInfo.hash
					val encodedBytes = this.blocks.get(hash).get
					result.append(Block.decode(encodedBytes))
				}
				result.toSeq
			case None =>
				result.toSeq
		}
	}

	override def getChainBlockByNumber(blockNumber: Long): Option[Block] = {
		Option(this.cache).flatMap(_.getChainBlockByNumber(blockNumber)) match {
			case Some(block) => Some(block)
			case None =>
				this.index.get(blockNumber) match {
					case Some(blockInfoBuffer) =>
						for (blockInfo <- blockInfoBuffer) {
							if (blockInfo.mainChain) {
								val encodedBytes = this.blocks.get(blockInfo.hash).get
								return Some(Block.decode(encodedBytes))
							}
						}
						None
					case None =>
						None
				}
		}
	}

	override def getBlockByHash(hash: ImmutableBytes): Option[Block] = {
		Option(this.cache).flatMap(_.getBlockByHash(hash)) match {
			case Some(block) => Some(block)
			case None =>
				this.blocks.get(hash) match {
					case Some(bytes) => Option(Block.decode(bytes))
					case None => None
				}
		}
	}

	override def existsBlock(hash: ImmutableBytes): Boolean = {
		Option(this.cache).flatMap(_.getBlockByHash(hash)) match {
			case Some(block) => true
			case None => this.blocks.get(hash).isDefined
		}
	}

	override def getTotalDifficultyForHash(hash: ImmutableBytes) = ???

	override def getTotalDifficulty = ???

	override def getMaxNumber: Long = {
		val bestIndex = 0.max(this.index.size).toLong
		if (this.cache ne null) {
			bestIndex + this.cache.index.size - 1L
		} else {
			bestIndex - 1L
		}
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
