package org.lipicalabs.lipica.core.db

import java.io._

import org.lipicalabs.lipica.core.kernel.Block
import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.mapdb.{DataIO, Serializer, DB}
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}
import scala.collection.mutable.ArrayBuffer


/**
 * Created by IntelliJ IDEA.
 * 2015/11/19 20:36
 * YANAGISAWA, Kentaro
 */
class IndexedBlockStore(private val index: mutable.Map[Long, Seq[BlockInfo]], private val blocks: KeyValueDataSource, private val cache: IndexedBlockStore, private val indexDB: DB) extends AbstractBlockStore {

	import IndexedBlockStore._

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
		if (Option(this.cache).isEmpty) {
			return
		}

		val startTime = System.nanoTime
		for (key <- this.cache.blocks.keys) {
			this.blocks.put(key, this.cache.blocks.get(key).get)
		}
		val time1 = System.nanoTime

		for (entry <- this.cache.index) {
			val (number, cachedInfoSeq) = entry

			val infoSeq = cachedInfoSeq ++ this.index.getOrElse(number, Seq.empty[BlockInfo])
			this.index.put(number, infoSeq)
		}
		val time2 = System.nanoTime

		this.cache.blocks.close()
		this.cache.index.clear()
		this.indexDB.commit()
		val endTime = System.nanoTime
		logger.info("<IndexBlockStore> Flushed block store in %,d nanos (Blocks=%,d nanos; Indices=%,d nanos; Commit=%,d nanos.".format(
			endTime - startTime, time1 - startTime, time2 - time1, endTime - time2
		))
	}

	/**
	 * ブロックを登録します。
	 */
	override def saveBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean) = {
		if (Option(this.cache).isEmpty) {
			addInternalBlock(block, cumulativeDifficulty, mainChain)
		} else {
			this.cache.saveBlock(block, cumulativeDifficulty, mainChain)
		}
	}

	/**
	 * ブロックを自分自身に登録します。
	 */
	private def addInternalBlock(block: Block, cumulativeDifficulty: BigInt, mainChain: Boolean): Unit = {
		//すでに同じブロック番号の元に登録されているブロック群。
		val blockInfoBuffer = this.index.getOrElse(block.blockNumber, Seq.empty[BlockInfo]).toBuffer
		blockInfoBuffer.append(new BlockInfo(block.hash, cumulativeDifficulty, mainChain))

		//覚える。
		this.index.put(block.blockNumber, blockInfoBuffer.toSeq)
		this.blocks.put(block.hash, block.encode)
	}

	/**
	 * メインのチェーンから、指定された番号のブロックを探し、そのハッシュ値を返します。
	 */
	override def getBlockHashByNumber(blockNumber: Long): Option[ImmutableBytes] = getChainBlockByNumber(blockNumber).map(_.hash)

	def getBlocksByNumber(number: Long): Seq[Block] = {
		//キャッシュ由来分。
		val result =
			if (Option(this.cache).isDefined) {
				this.cache.getBlocksByNumber(number).toBuffer
			} else {
				new ArrayBuffer[Block]
			}
		//自身由来分。
		this.index.get(number) match {
			case Some(blockInfoSeq) =>
				for (blockInfo <- blockInfoSeq) {
					this.blocks.get(blockInfo.hash).foreach(encodedBytes => result.append(Block.decode(encodedBytes)))
				}
				result.toSeq
			case None =>
				result.toSeq
		}
	}

	/**
	 * メインのチェーンから、渡された番号を持つブロックを探索して返します。
	 */
	override def getChainBlockByNumber(blockNumber: Long): Option[Block] = {
		Option(this.cache).flatMap(_.getChainBlockByNumber(blockNumber)) match {
			case Some(block) => Some(block)
			case None =>
				//キャッシュにない。
				this.index.get(blockNumber) match {
					case Some(blockInfoSeq) =>
						for (blockInfo <- blockInfoSeq) {
							if (blockInfo.mainChain) {
								return Some(Block.decode(this.blocks.get(blockInfo.hash).get))
							}
						}
						None
					case None =>
						None
				}
		}
	}

	/**
	 * 渡されたハッシュ値を持つブロックを探索して返します。
	 */
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

	/**
	 * 渡されたハッシュ値を持つブロックが存在するか否かを返します。
	 */
	override def existsBlock(hash: ImmutableBytes): Boolean = {
		Option(this.cache).flatMap(_.getBlockByHash(hash)) match {
			case Some(block) => true
			case None => this.blocks.get(hash).isDefined
		}
	}

	override def getTotalDifficultyForHash(hash: ImmutableBytes): BigInt = {
		Option(this.cache).flatMap(_.getBlockByHash(hash)) match {
			case Some(_) =>
				//キャッシュにある。
				this.cache.getTotalDifficultyForHash(hash)
			case None =>
				//キャッシュにない。
				getBlockByHash(hash) match {
					case Some(block) =>
						val level = block.blockNumber
						val blockInfoSeq = this.index.get(level).get
						blockInfoSeq.find(_.hash == hash).map(_.cumulativeDifficulty).getOrElse(UtilConsts.Zero)
					case None =>
						UtilConsts.Zero
				}
		}
	}

	private def getBlockInfoSeqForLevel(level: Long): Option[Seq[BlockInfo]] = {
		Option(this.cache).flatMap(_.index.get(level)) match {
			case Some(seq) => Some(seq)
			case None => this.index.get(level)
		}
	}

	override def getTotalDifficulty: BigInt = {
		if (Option(this.cache).isDefined) {
			val blockInfoSeqOrNone = getBlockInfoSeqForLevel(getMaxBlockNumber)
			if (blockInfoSeqOrNone.isDefined) {
				val foundOrNone = blockInfoSeqOrNone.get.find(_.mainChain)
				if (foundOrNone.isDefined) {
					return foundOrNone.get.cumulativeDifficulty
				}
				var number = getMaxBlockNumber
				while (0 <= number) {
					number -= 1
					getBlockInfoSeqForLevel(number).foreach {
						eachSeq => {
							val foundOrNone2 = eachSeq.find(_.mainChain)
							if (foundOrNone2.isDefined) {
								return foundOrNone2.get.cumulativeDifficulty
							}
						}
					}
				}
			}
		}
		val blockInfoSeq = this.index.get(getMaxBlockNumber).get
		blockInfoSeq.find(_.mainChain) match {
			case Some(block) => block.cumulativeDifficulty
			case None => UtilConsts.Zero
		}
	}

	/**
	 * 最大のブロック番号を返します。
	 */
	override def getMaxBlockNumber: Long = {
		val bestIndex = 0.max(this.index.size).toLong
		if (Option(this.cache).isDefined) {
			bestIndex + this.cache.index.size - 1L
		} else {
			bestIndex - 1L
		}
	}

	/**
	 * 渡されたハッシュ値を持つブロック以前のブロックのハッシュ値を並べて返します。
	 * 並び順は、最も新しい（＝ブロック番号が大きい）ブロックを先頭として過去に遡行する順序となります。
	 */
	override def getHashesEndingWith(hash: ImmutableBytes, number: Long): Seq[ImmutableBytes] = {
		val seq =
			if (Option(this.cache).isDefined) {
				this.cache.getHashesEndingWith(hash, number)
			} else {
				Seq.empty[ImmutableBytes]
			}
		this.blocks.get(hash) match {
			case Some(bytes) =>
				val buffer = seq.toBuffer
				var encodedBytes = bytes
				for (i <- 0L until number) {
					val block = Block.decode(encodedBytes)
					buffer.append(block.hash)
					//親をたどって、後続に追加する。
					blocks.get(block.parentHash) match {
						case Some(b) =>
							encodedBytes = b
						case None =>
							return buffer.toSeq
					}
				}
				buffer.toSeq
			case None =>
				seq
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
			this.index.get(number + i) match {
				case Some(blockInfoSeq) =>
					blockInfoSeq.find(_.mainChain).foreach(b => result.append(b.hash))
				case None =>
					shouldContinue = false
			}
			i += 1
		}
		val maxBlocks = aMaxBlocks - i
		Option(this.cache).foreach {
			c => result.appendAll(c.getHashesStartingWith(number, maxBlocks))
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
				val blockInfoSeq = getBlockInfoSeqForLevel(currentLevel).get
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
				val blockInfoSeq = getBlockInfoSeqForLevel(currentLevel).get
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
			val levelBlocks = getBlockInfoSeqForLevel(currentLevel).get
			getBlockInfoForHash(levelBlocks, bestLine.hash).foreach(_.mainChain = false)
			getBlockInfoForHash(levelBlocks, forkLine.hash).foreach(_.mainChain = true)

			bestLine = getBlockByHash(bestLine.parentHash).get
			forkLine = getBlockByHash(forkLine.parentHash).get

			currentLevel -= 1
		}
	}

	private def getBlockInfoForHash(blocks: Seq[BlockInfo], hash: ImmutableBytes): Option[BlockInfo] = blocks.find(_.hash == hash)

	override def load(): Unit = {
		//
	}
}

object IndexedBlockStore {
	private val logger = LoggerFactory.getLogger("database")

	def newInstance(index: mutable.Map[Long, Seq[BlockInfo]], blocks: KeyValueDataSource, cache: IndexedBlockStore, indexDB: DB): IndexedBlockStore = new IndexedBlockStore(index, blocks, cache, indexDB)

	def newInstance(index: mutable.Map[Long, Seq[BlockInfo]], blocks: KeyValueDataSource): IndexedBlockStore = new IndexedBlockStore(index, blocks, null, null)

}

class BlockInfo(private var _hash: ImmutableBytes, private var _cumulativeDifficulty: BigInt, private var _mainChain: Boolean) extends Serializable {

	def hash: ImmutableBytes = this._hash
	def hash_=(v: ImmutableBytes): Unit = this._hash = v

	def cumulativeDifficulty: BigInt = this._cumulativeDifficulty
	def cumulativeDifficulty_=(v: BigInt): Unit = this._cumulativeDifficulty = v

	def mainChain: Boolean = this._mainChain
	def mainChain_=(v: Boolean): Unit = this._mainChain = v
}

object BlockInfoSerializer extends Serializer[Seq[BlockInfo]] {

	override def serialize(out: DataOutput, value: Seq[BlockInfo]): Unit = {
		import JavaConversions._
		val outputStream = new ByteArrayOutputStream()
		val objectOutputStream = new ObjectOutputStream(outputStream)

		objectOutputStream.writeObject(seqAsJavaList(value))
		objectOutputStream.flush()
		val data = outputStream.toByteArray
		DataIO.packInt(out, data.length)
		out.write(data)
	}

	override def deserialize(in: DataInput, available: Int): Seq[BlockInfo] = {
		import JavaConversions._

		val size = DataIO.unpackInt(in)
		val data = new Array[Byte](size)
		in.readFully(data)

		val inputStream = new ByteArrayInputStream(data, 0, data.length)
		val objectInputStream = new ObjectInputStream(inputStream)
		val value = objectInputStream.readObject().asInstanceOf[java.util.List[BlockInfo]]
		value
	}

}