package org.lipicalabs.lipica.core.db

import java.util.concurrent.Executors
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.kernel.BlockWrapper
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.datasource.mapdb.{Serializers, MapDBFactory}
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.mapdb.{Serializer, DB}
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:49
 * YANAGISAWA, Kentaro
 */
class BlockQueueImpl(private val mapDBFactory: MapDBFactory) extends BlockQueue {

	import BlockQueueImpl._
	import scala.collection.JavaConversions._

	private var readHits: Int = 0
	private var db: DB = null
	private var blocks: mutable.Map[Long, BlockWrapper] = null
	private var hashes: mutable.Set[ImmutableBytes] = null
	private var index: Index = null

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
					BlockQueueImpl.this.db = BlockQueueImpl.this.mapDBFactory.createTransactionalDB(dbName)
					BlockQueueImpl.this.blocks = mapAsScalaMap(BlockQueueImpl.this.db.hashMapCreate(StoreName).keySerializer(Serializer.LONG).valueSerializer(Serializers.BlockWrapper).makeOrGet())
					BlockQueueImpl.this.hashes = asScalaSet(BlockQueueImpl.this.db.hashSetCreate(HashSetName).serializer(Serializers.ImmutableBytes).makeOrGet())

					if (SystemProperties.CONFIG.databaseReset) {
						BlockQueueImpl.this.blocks.clear()
						BlockQueueImpl.this.hashes.clear()
						BlockQueueImpl.this.db.commit()
					}

					BlockQueueImpl.this.index = new ArrayBufferIndex(BlockQueueImpl.this.blocks.keys)
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

	override def addAll(aBlocks: Iterable[BlockWrapper]): Unit = {
		awaitInit()
		this.writeMutex.synchronized {
			val numbers = new ArrayBuffer[Long](aBlocks.size)
			val newHashes = new mutable.HashSet[ImmutableBytes]
			aBlocks.withFilter(b => !this.index.contains(b.blockNumber) && !numbers.contains(b.blockNumber)).foreach {
				block => {
					this.blocks.put(block.blockNumber, block)
					numbers.append(block.blockNumber)
					newHashes.add(block.hash)
				}
			}
			this.hashes.addAll(newHashes)

			this.takeLock.lock()
			try {
				this.index.addAll(numbers)
				this.notEmpty.signalAll()
			} finally {
				this.takeLock.unlock()
			}
		}
		this.db.commit()
	}

	override def add(block: BlockWrapper): Unit = {
		awaitInit()
		this.writeMutex.synchronized {
			if (this.index.contains(block.blockNumber)) {
				return
			}
			this.blocks.put(block.blockNumber, block)
			this.hashes.add(block.hash)

			this.takeLock.lock()
			try {
				this.index.add(block.blockNumber)
				this.notEmpty.signalAll()
			} finally {
				this.takeLock.unlock()
			}
		}
		this.db.commit()
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
			this.blocks.get(idx)
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
			this.blocks.clear()
			this.hashes.clear()
			this.index.clear()
		}
		this.db.commit()
	}

	/**
	 * 渡されたハッシュ値の中から、既にこのキューに溜まっているものを除外したものを返します。
	 */
	override def excludeExisting(aHashes: Seq[ImmutableBytes]): Seq[ImmutableBytes] = {
		awaitInit()
		aHashes.filter(each => !this.hashes.contains(each))
	}

	private def pollInternal: Option[BlockWrapper] = {
		this.readMutex.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val idx = this.index.poll
			val blockOrNone = this.blocks.get(idx)
			this.blocks.remove(idx)
			blockOrNone.foreach {
				block => this.hashes.remove(block.hash)
			}
			blockOrNone
		}
	}

	private def commitReading(): Unit = {
		this.readHits += 1
		if (ReadHitsCommitThreshold <= this.readHits) {
			this.db.commit()
			this.readHits = 0
		}
	}

	private def dbName: String = "%s/%s".format(StoreName, StoreName)

	override def close(): Unit = {
		awaitInit()
		this.db.close()
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

	private val logger = LoggerFactory.getLogger("blockqueue")
	private val ReadHitsCommitThreshold: Int = 1000
	private val StoreName: String = "blockqueue"
	private val HashSetName: String = "hashset"

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
