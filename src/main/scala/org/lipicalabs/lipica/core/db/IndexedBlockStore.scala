package org.lipicalabs.lipica.core.db

import java.io._
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicLong, AtomicBoolean}

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.kernel.Block
import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}
import scala.collection.mutable.ArrayBuffer


/**
 * ブロック番号とブロックハッシュ値のそれぞれをキーとして
 * ブロックを保存・探索する、ノードの中核的なデータストアクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/19 20:36
 * YANAGISAWA, Kentaro
 */
class IndexedBlockStore private(private val hashToBlockStore: KeyValueDataSource, private val numberToBlocksStore: KeyValueDataSource) extends AbstractBlockStore {

	import IndexedBlockStore._
	import JavaConversions._

	//TODO 永続化しなければならない。
	private val maxBlockNumberRef = new AtomicLong(-1L)

	private val hashToBlockCache = mapAsScalaMap(new ConcurrentHashMap[ImmutableBytes, ImmutableBytes])
	private val numberToBlocksCache = mapAsScalaMap(new ConcurrentHashMap[Long, Seq[BlockInfo]])

	private def readThroughByHash(hash: ImmutableBytes): Option[Block] = {
		this.hashToBlockCache.get(hash) match {
			case Some(encoded) =>
				Some(Block.decode(encoded))
			case None =>
				this.hashToBlockStore.get(hash) match {
					case Some(bytes) =>
						Some(Block.decode(bytes))
					case None =>
						None
				}
		}
	}

	private def readThroughByBlockNumber(blockNumber: Long): Seq[BlockInfo] = {
		val cached = this.numberToBlocksCache.getOrElse(blockNumber, Seq.empty)
		val loaded = this.numberToBlocksStore.get(RBACCodec.Encoder.encode(blockNumber)).map(bytes => decodeToSeqOfBlockInfo(bytes)).getOrElse(Seq.empty)
		cached ++ loaded
	}

	private def decodeToSeqOfBlockInfo(bytes: ImmutableBytes): Seq[BlockInfo] = {
		val items = RBACCodec.Decoder.decode(bytes).right.get.items
		items.map(each => BlockInfo.decode(each.items))
	}

	private def encodeToBytes(blockInfoSeq: Seq[BlockInfo]): ImmutableBytes = {
		val seqOfBytes = blockInfoSeq.map(each => each.encode)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seqOfBytes)
	}

	/**
	 * 最大番号のブロックを取得します。
	 */
	override def getBestBlock: Option[Block] = {
		val maxLevel = getMaxBlockNumber
		if (maxLevel < 0) {
			return None
		}
		var nextLevel = maxLevel
		while (0 <= nextLevel) {
			//メインのチェーンに属するブロックを取得する。
			getChainBlockByNumber(nextLevel) match {
				case Some(block) => return Some(block)
				case None =>
			}
			nextLevel -= 1
		}
		None
	}

	/**
	 * ディスクに永続化します。
	 */
	override def flush(): Unit = {
		this.synchronized {
			var numBlocks = 0
			val startTime = System.nanoTime
			val temporaryHashToBlockMap = new mutable.HashMap[ImmutableBytes, ImmutableBytes]
			for (entry <- this.hashToBlockCache) {
				temporaryHashToBlockMap.put(entry._1, entry._2)
				numBlocks += 1
			}
			this.hashToBlockStore.updateBatch(temporaryHashToBlockMap.toMap)
			val time1 = System.nanoTime

			var numIndices = 0
			val temporaryNumberToBlocksMap = new mutable.HashMap[ImmutableBytes, ImmutableBytes]
			for (number <- this.numberToBlocksCache.keys) {
				val infoSeq = readThroughByBlockNumber(number)
				temporaryNumberToBlocksMap.put(RBACCodec.Encoder.encode(number), encodeToBytes(infoSeq))
				numIndices += 1
			}
			this.numberToBlocksStore.updateBatch(temporaryNumberToBlocksMap.toMap)
			this.hashToBlockCache.clear()
			this.numberToBlocksCache.clear()
			val endTime = System.nanoTime

			//		println("<IndexBlockStore> Flushed block store in %,d nanos (%,d blocks in %,d nanos; %,d indices in %,d nanos.".format(
			//			endTime - startTime, numBlocks, time1 - startTime, numIndices, endTime - time1
			//		))

			logger.info("<IndexBlockStore> Flushed block store in %,d nanos (%,d blocks in %,d nanos; %,d indices in %,d nanos.".format(
				endTime - startTime, numBlocks, time1 - startTime, numIndices, endTime - time1
			))
		}
	}

	/**
	 * ブロックを登録します。
	 */
	override def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean) = {
		this.synchronized {
			//すでに同じブロック番号の元に登録されているブロック群。
			val blockInfoBuffer = this.numberToBlocksCache.getOrElse(block.blockNumber, Seq.empty[BlockInfo]).toBuffer
			blockInfoBuffer.append(new BlockInfo(block.hash, cumulativeDifficulty, mainChain))

			//覚える。
			this.numberToBlocksCache.put(block.blockNumber, blockInfoBuffer.toSeq)
			this.hashToBlockCache.put(block.hash, block.encode)

			val current = this.maxBlockNumberRef.get
			if (current < block.blockNumber) {
				this.maxBlockNumberRef.set(block.blockNumber)
			}
		}
	}

	/**
	 * メインのチェーンから、指定された番号のブロックを探し、そのハッシュ値を返します。
	 */
	override def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes] = getChainBlockByNumber(blockNumber).map(_.hash)

	def getBlocksByNumber(number: Long): Seq[Block] = {
		readThroughByBlockNumber(number).flatMap(blockInfo => this.readThroughByHash(blockInfo.hash))
	}

	/**
	 * メインのチェーンから、渡された番号を持つブロックを探索して返します。
	 */
	override def getChainBlockByNumber(blockNumber: Long): Option[Block] = {
		readThroughByBlockNumber(blockNumber).find(_.mainChain).flatMap(blockInfo => this.readThroughByHash(blockInfo.hash))
	}

	/**
	 * 渡されたハッシュ値を持つブロックを探索して返します。
	 */
	override def getBlockByHash(hash: ImmutableBytes): Option[Block] = {
		readThroughByHash(hash)
	}

	/**
	 * 渡されたハッシュ値を持つブロックが存在するか否かを返します。
	 */
	override def existsBlock(hash: ImmutableBytes): Boolean = {
		readThroughByHash(hash).isDefined
	}

	override def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt = {
		readThroughByHash(hash).flatMap(block => readThroughByBlockNumber(block.blockNumber).find(blockInfo => blockInfo.hash == block.hash)).map(_.cumulativeDifficulty).getOrElse(UtilConsts.Zero)
	}

	override def getTotalDifficulty: BigInt = {
		var number = getMaxBlockNumber
		while (0 <= number) {
			readThroughByBlockNumber(number).find(_.mainChain).foreach {
				found => return found.cumulativeDifficulty
			}
			number -= 1
		}
		UtilConsts.Zero
	}

	/**
	 * 最大のブロック番号を返します。
	 */
	override def getMaxBlockNumber: Long = {
		this.maxBlockNumberRef.get
	}

	/**
	 * 渡されたハッシュ値を持つブロック以前のブロックのハッシュ値を並べて返します。
	 * 並び順は、最も新しい（＝ブロック番号が大きい）ブロックを先頭として過去に遡行する順序となります。
	 */
	override def getHashesEndingWith(hash: ImmutableBytes, number: Long): Seq[ImmutableBytes] = {
		readThroughByHash(hash) match {
			case Some(initialBlock) =>
				val buffer = new ArrayBuffer[ImmutableBytes]
				var eachBlock = initialBlock
				for (i <- 0L until number) {
					buffer.append(eachBlock.hash)
					//親をたどって、後続に追加する。
					readThroughByHash(eachBlock.parentHash) match {
						case Some(b) =>
							eachBlock = b
						case None =>
							return buffer.toSeq
					}
				}
				buffer.toSeq
			case None =>
				Seq.empty
		}
	}

	/**
	 * genesisから始まって指定された個数のブロックのハッシュ値を並べて返します。
	 */
	def getHashesStartingWith(number: Long, aMaxBlocks: Long): Seq[ImmutableBytes] = {
		val result = new ArrayBuffer[ImmutableBytes]
		var i = 0
		var shouldContinue = true
		while ((i < aMaxBlocks) && shouldContinue) {
			val blockInfoSeq = readThroughByBlockNumber(number + i)
			blockInfoSeq.find(_.mainChain).foreach(b => result.append(b.hash))
			shouldContinue = blockInfoSeq.nonEmpty
			i += 1
		}
		result.toSeq
	}

	/**
	 * 本線と分線との関係を変更します。
	 */
	override def rebranch(forkBlock: Block): Unit = {
		//メインのチェーンに属する最大番号ブロック。
		val bestBlock = getBestBlock.get
		val maxLevel = bestBlock.blockNumber max forkBlock.blockNumber

		var currentLevel = maxLevel
		var forkLine = forkBlock
		if (bestBlock.blockNumber < forkBlock.blockNumber) {
			//フォークと見なしていた方が、より長く成長している。
			while (bestBlock.blockNumber < currentLevel) {
				val blockInfoSeq = readThroughByBlockNumber(currentLevel)
				getBlockInfoForHash(blockInfoSeq, forkLine.hash).foreach {
					blockInfo => {
						//このラインを本線にする。
						blockInfo.mainChain = true
						forkLine = getBlockByHash(forkLine.parentHash).get
					}
				}
				currentLevel -= 1
			}
		}

		var bestLine = bestBlock
		if (forkBlock.blockNumber < bestBlock.blockNumber) {
			while (forkBlock.blockNumber < currentLevel) {
				val blockInfoSeq = readThroughByBlockNumber(currentLevel)
				getBlockInfoForHash(blockInfoSeq, bestLine.hash).foreach {
					blockInfo => {
						//このラインをフォークにする。
						blockInfo.mainChain = false
						bestLine = getBlockByHash(bestLine.parentHash).get
					}
				}
				currentLevel -= 1
			}
		}

		//共通の祖先に至るまで、遡って本線と分線とを付け替える。
		while (bestLine.hash != forkLine.hash) {
			val levelBlocks = readThroughByBlockNumber(currentLevel)
			getBlockInfoForHash(levelBlocks, bestLine.hash).foreach(_.mainChain = false)
			getBlockInfoForHash(levelBlocks, forkLine.hash).foreach(_.mainChain = true)

			bestLine = getBlockByHash(bestLine.parentHash).get
			forkLine = getBlockByHash(forkLine.parentHash).get

			currentLevel -= 1
		}
	}

	private def getBlockInfoForHash(blocks: Seq[BlockInfo], hash: ImmutableBytes): Option[BlockInfo] = blocks.find(_.hash == hash)

	override def close(): Unit = {
		this.synchronized {
			this.hashToBlockStore.close()
			this.numberToBlocksStore.close()
		}
	}
}

object IndexedBlockStore {
	private val logger = LoggerFactory.getLogger("database")

	def newInstance(hashToBlockStore: KeyValueDataSource, numberToBlocksStore: KeyValueDataSource): IndexedBlockStore = new IndexedBlockStore(hashToBlockStore, numberToBlocksStore)

}

class BlockInfo(val hash: ImmutableBytes, val cumulativeDifficulty: BigInt, _mainChain: Boolean) extends Serializable {
	private val mainChainRef = new AtomicBoolean(_mainChain)
	def mainChain: Boolean = this.mainChainRef.get
	def mainChain_=(v: Boolean): Unit = this.mainChainRef.set(v)

	def encode: ImmutableBytes = {
		val encoder = RBACCodec.Encoder
		val seqOfBytes = Seq(encoder.encode(hash), encoder.encode(cumulativeDifficulty), encoder.encode(mainChain))
		encoder.encodeSeqOfByteArrays(seqOfBytes)
	}
}

object BlockInfo {
	def decode(encoded: ImmutableBytes): BlockInfo = {
		val items = RBACCodec.Decoder.decode(encoded).right.get.items
		decode(items)
	}

	def decode(items: Seq[DecodedResult]): BlockInfo = {
		val hash = items.head.bytes
		val cumulativeDifficulty = items(1).asPositiveBigInt
		val mainChain = 0 < items(2).asInt
		new BlockInfo(hash, cumulativeDifficulty, mainChain)
	}
}
