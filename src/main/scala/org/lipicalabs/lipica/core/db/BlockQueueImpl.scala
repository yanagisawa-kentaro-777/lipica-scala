package org.lipicalabs.lipica.core.db

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.kernel.BlockWrapper
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.lipicalabs.lipica.utils.MiscUtils
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * ブロックを処理するための producer-consumer queue の実装です。
 *
 * ただし、ブロックを取り出す際に必ず小さい番号のブロックから順に取り出すため、
 * 内部でソートを行っています。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:49
 * YANAGISAWA, Kentaro
 */
class BlockQueueImpl(private val blocksDataSource: KeyValueDataSource, private val hashesDataSource: KeyValueDataSource) extends BlockQueue {

	import BlockQueueImpl._

	//takeLock等によってガードされている。
	private var readHits: Int = 0

//	private val dbRef: AtomicReference[DB] = new AtomicReference[DB](null)
//	private def db: DB = this.dbRef.get

//	//takeLock等によってガードされている。
//	private var blocks: mutable.Map[Long, BlockWrapper] = null
//	private var hashes: mutable.Set[ImmutableBytes] = null

	private val indexRef: AtomicReference[Index] = new AtomicReference[Index](null)
	private def index: Index = this.indexRef.get

	// initLock によってガードされている。
	private var initDone: Boolean = false
	private val initLock = new ReentrantLock
	private val init = initLock.newCondition

	private val takeLock = new ReentrantLock
	private val notEmpty = takeLock.newCondition

	private val writeMutex = new Object
	private val readMutex = new Object

	override def open(): Unit = {
		val task = new Runnable() {
			override def run(): Unit = {
				BlockQueueImpl.this.initLock.lock()
				try {
					//BlockQueueImpl.this.dbRef.set(BlockQueueImpl.this.mapDBFactory.createTransactionalDB(dbName))
					//BlockQueueImpl.this.blocks = mapAsScalaMap(BlockQueueImpl.this.db.hashMapCreate(StoreName).keySerializer(Serializer.LONG).valueSerializer(Serializers.BlockWrapper).makeOrGet())
					//BlockQueueImpl.this.hashes = asScalaSet(BlockQueueImpl.this.db.hashSetCreate(HashSetName).serializer(Serializers.ImmutableBytes).makeOrGet())

					if (SystemProperties.CONFIG.databaseReset) {
//						BlockQueueImpl.this.blocks.clear()
//						BlockQueueImpl.this.hashes.clear()
//						BlockQueueImpl.this.db.commit()
					}

					BlockQueueImpl.this.indexRef.set(new ArrayBufferIndex(BlockQueueImpl.this.blocksDataSource.keys.map(each => RBACCodec.Decoder.decode(each).right.get.asPositiveLong)))
					BlockQueueImpl.this.initDone = true
					BlockQueueImpl.this.readHits = 0
					BlockQueueImpl.this.init.signalAll()

					logger.info("<BlockQueueImpl> Block queue is loaded, size[%d]".format(BlockQueueImpl.this.size))
				} finally {
					BlockQueueImpl.this.initLock.unlock()
				}
			}
		}
		Executors.newSingleThreadExecutor(new CountingThreadFactory("block-queue")).execute(task)
	}

	private def getBlock(blockNumber: Long): Option[BlockWrapper] = {
		val key = RBACCodec.Encoder.encode(blockNumber)
		this.blocksDataSource.get(key).map(encoded => BlockWrapper.parse(encoded))
	}

	private def putBlock(blockWrapper: BlockWrapper): Unit = {
		val key = RBACCodec.Encoder.encode(blockWrapper.blockNumber)
		val value = blockWrapper.toBytes
		this.blocksDataSource.put(key, value)
	}

	private def deleteBlock(blockNumber: Long): Unit = {
		val key = RBACCodec.Encoder.encode(blockNumber)
		this.blocksDataSource.delete(key)
	}

	private def existsHash(aHash: ImmutableBytes): Boolean = {
		this.hashesDataSource.get(aHash).isDefined
	}

	private def putHash(aHash: ImmutableBytes): Unit = {
		this.hashesDataSource.put(aHash, OneByteValue)
	}

	private def putHashes(aHashes: Iterable[ImmutableBytes]): Unit = {
		val rows = aHashes.map(each => (each, OneByteValue)).toMap
		this.hashesDataSource.updateBatch(rows)
	}

	private def deleteHash(aHash: ImmutableBytes): Unit = {
		this.hashesDataSource.delete(aHash)
	}

	override def addAll(aBlocks: Iterable[BlockWrapper]): Unit = {
		awaitInit()
		this.writeMutex.synchronized {
			val numbers = new ArrayBuffer[Long](aBlocks.size)
			val newHashes = new mutable.HashSet[ImmutableBytes]
			aBlocks.withFilter(b => !this.index.contains(b.blockNumber) && !numbers.contains(b.blockNumber)).foreach {
				block => {
					putBlock(block)
					numbers.append(block.blockNumber)
					newHashes.add(block.hash)
				}
			}
			putHashes(newHashes)

			this.takeLock.lock()
			try {
				this.index.addAll(numbers)
				this.notEmpty.signalAll()
			} finally {
				this.takeLock.unlock()
			}
		}
	}

	override def add(block: BlockWrapper): Unit = {
		awaitInit()
		this.writeMutex.synchronized {
			if (this.index.contains(block.blockNumber)) {
				return
			}
			putBlock(block)
			putHash(block.hash)

			this.takeLock.lock()
			try {
				this.index.add(block.blockNumber)
				this.notEmpty.signalAll()
			} finally {
				this.takeLock.unlock()
			}
		}
	}

	override def poll: Option[BlockWrapper] = {
		awaitInit()
		val block = pollInternal
		commitReading()
		block
	}

	override def peek: Option[BlockWrapper] = {
		awaitInit()
		this.readMutex.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val idx = this.index.peek
			getBlock(idx)
		}
	}

	override def take: BlockWrapper = {
		awaitInit()
		this.takeLock.lock()
		try {
			var blockOrNone = pollInternal
			while (blockOrNone.isEmpty) {
				this.notEmpty.awaitUninterruptibly()
				blockOrNone = pollInternal
			}
			commitReading()
			blockOrNone.get
		} finally {
			this.takeLock.unlock()
		}
	}

	override def size: Int = {
		awaitInit()
		this.index.size
	}

	override def isEmpty: Boolean = {
		awaitInit()
		this.index.isEmpty
	}

	override def nonEmpty: Boolean = !this.isEmpty

	override def clear(): Unit = {
		awaitInit()
		this.synchronized {
			this.blocksDataSource.keys.foreach(this.blocksDataSource.delete)
			this.hashesDataSource.keys.foreach(this.hashesDataSource.delete)
			this.index.clear()
		}
	}

	/**
	 * 渡されたハッシュ値の中から、既にこのキューに溜まっているものを除外したものを返します。
	 */
	override def excludeExisting(aHashes: Seq[ImmutableBytes]): Seq[ImmutableBytes] = {
		awaitInit()
		aHashes.filter(each => !existsHash(each))
	}

	private def pollInternal: Option[BlockWrapper] = {
		this.readMutex.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val idx = this.index.poll
			val blockOrNone = getBlock(idx)
			deleteBlock(idx)
			blockOrNone.foreach {
				block => deleteHash(block.hash)
			}
			blockOrNone
		}
	}

	private def commitReading(): Unit = {
		this.readHits += 1
		if (ReadHitsCommitThreshold <= this.readHits) {
			this.readHits = 0
		}
	}

	override def close(): Unit = {
		awaitInit()
		MiscUtils.closeIfNotNull(this.blocksDataSource)
		MiscUtils.closeIfNotNull(this.hashesDataSource)
		this.initDone = false
	}

	private def awaitInit(): Unit = {
		this.initLock.lock()
		try {
			if (!this.initDone) {
				this.init.awaitUninterruptibly()
			}
		} finally {
			this.initLock.unlock()
		}
	}

}

object BlockQueueImpl {

	private val logger = LoggerFactory.getLogger("database")
	private val ReadHitsCommitThreshold: Int = 1000

	private val OneByteValue = ImmutableBytes.fromOneByte(0)

	trait Index {
		def addAll(nums: Iterable[Long]): Unit
		def add(v: Long): Unit
		def peek: Long
		def poll: Long

		def contains(v: Long): Boolean
		def isEmpty: Boolean
		def nonEmpty: Boolean
		def size: Int
		def clear(): Unit
	}

	class ArrayBufferIndex(numbers: Iterable[Long]) extends Index {
		private var index = new ArrayBuffer[Long]
		this.synchronized {
			index.appendAll(numbers)
			sort()
		}

		override def addAll(nums: Iterable[Long]) = {
			this.synchronized {
				this.index.appendAll(nums)
				sort()
			}
		}

		override def add(v: Long) = {
			this.synchronized {
				this.index.append(v)
				sort()
			}
		}

		override def peek = {
			this.synchronized {
				this.index.head
			}
		}

		override def poll = {
			this.synchronized {
				val result = this.index.head
				this.index.remove(0)
				result
			}
		}

		override def contains(v: Long) = {
			this.synchronized {
				binarySearch(v, this.index, 0, this.index.size - 1).isDefined
			}
		}

		override def clear() = {
			this.synchronized {
				this.index.clear()
			}
		}

		override def size = {
			this.synchronized {
				this.index.size
			}
		}


		override def isEmpty = {
			this.synchronized {
				this.index.isEmpty
			}
		}

		override def nonEmpty = {
			this.synchronized {
				this.index.nonEmpty
			}
		}

		private def sort(): Unit = {
			this.index = this.index.sorted
		}

		@tailrec
		private def binarySearch(key: Long, xs: mutable.Buffer[Long], min: Int, max: Int): Option[Long] = {
			if (max < min)
				None
			else {
				val mid = (min + max) >>> 1
				val extracted = xs(mid)
				if (key < extracted)
					binarySearch(key, xs, min, mid - 1)
				else if (extracted < key)
					binarySearch(key, xs, mid + 1, max)
				else
					Some(extracted)
			}
		}
	}

}

