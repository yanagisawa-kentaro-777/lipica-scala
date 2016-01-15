package org.lipicalabs.lipica.core.datastore

import java.util.concurrent.Executors
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/**
 * HashStore の実装クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/26 19:33
 * YANAGISAWA, Kentaro
 */
class HashStoreImpl(private val dataSource: KeyValueDataSource) extends HashStore {

	import HashStoreImpl._

	private val indexRef: AtomicReference[mutable.Buffer[Long]] = new AtomicReference[mutable.Buffer[Long]](null)
	private def index: mutable.Buffer[Long] = this.indexRef.get

	private val initDoneRef: AtomicBoolean = new AtomicBoolean(false)
	private def initDone: Boolean = this.initDoneRef.get

	private val initLock = new ReentrantLock
	private val init = this.initLock.newCondition

	override def open(): Unit = {
		val task = new Runnable() {
			override def run(): Unit = {
				initLock.lock()
				try {
					val indices = dataSource.keys.map(each => RBACCodec.Decoder.decode(each).right.get.asPositiveLong)
					val buffer = new ArrayBuffer[Long](initialSize = indices.size)
					buffer.appendAll(indices)
					indexRef.set(buffer.sorted)
					if (NodeProperties.CONFIG.databaseReset) {
						dataSource.deleteAll()
					}
					initDoneRef.set(true)
					init.signalAll()
					logger.info("<HashStore> Hash store loaded, size[%d]".format(size))
				} finally {
					initLock.unlock()
				}
			}
		}
		Executors.newSingleThreadExecutor(new CountingThreadFactory("hash-store")).execute(task)
	}

	override def close() = {
		awaitInit()
		this.dataSource.close()
		this.initDoneRef.set(false)
	}

	override def add(hash: ImmutableBytes) = {
		awaitInit()
		addInternal(first = false, hash)
	}

	override def addFirst(hash: ImmutableBytes) = {
		awaitInit()
		addInternal(first = true, hash)
	}

	override def addBatch(aHashes: Seq[ImmutableBytes]) = {
		privateAddBatch(aHashes, first = false)
	}

	override def addBatchFirst(aHashes: Seq[ImmutableBytes]) = {
		privateAddBatch(aHashes, first = true)
	}

	private def privateAddBatch(aHashes: Seq[ImmutableBytes], first: Boolean): Unit = {
		awaitInit()
		this.synchronized {
			val encoder = RBACCodec.Encoder
			val batch = aHashes.map(each => (encoder.encode(createIndex(first)), each)).toMap
			this.dataSource.updateBatch(batch)
		}
	}

	override def peek: Option[ImmutableBytes] = {
		awaitInit()
		this.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val key = RBACCodec.Encoder.encode(this.index.head)
			this.dataSource.get(key)
		}
	}

	override def poll: Option[ImmutableBytes] = {
		awaitInit()
		pollInternal
	}

	override def pollBatch(count: Int): Seq[ImmutableBytes] = {
		awaitInit()
		if (this.index.isEmpty) {
			return Seq.empty
		}
		val result = new ArrayBuffer[ImmutableBytes](count min this.size)
		var shouldContinue = true
		while (shouldContinue && (result.size < count)) {
			val each = pollInternal
			each.foreach(result.append(_))
			shouldContinue = each.isDefined
		}
		result.toSeq
	}

	override def isEmpty = {
		awaitInit()
		this.index.isEmpty
	}

	override def nonEmpty = !this.isEmpty

	override def keys = {
		awaitInit()
		this.dataSource.keys.map(_.toPositiveLong)
	}

	override def size = {
		awaitInit()
		this.index.size
	}

	override def clear() = {
		awaitInit()
		this.synchronized {
			this.index.clear()
			this.dataSource.deleteAll()
		}
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

	private def addInternal(first: Boolean, hash: ImmutableBytes): Unit = {
		this.synchronized {
			val idx = createIndex(first)
			val key = RBACCodec.Encoder.encode(idx)
			this.dataSource.put(key, hash)
		}
	}

	private def pollInternal: Option[ImmutableBytes] = {
		this.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val idx = this.index.head
			val key = RBACCodec.Encoder.encode(idx)
			val result = this.dataSource.get(key)
			this.dataSource.delete(key)
			this.index.remove(0)
			result
		}
	}

	private def createIndex(first: Boolean): Long = {
		var result = 0L
		if (this.index.isEmpty) {
			result = 0L
			this.index.append(result)
		} else if (first) {
			result = this.index.head - 1L
			this.index.insert(0, result)
		} else {
			result = this.index.last + 1L
			this.index.append(result)
		}
		result
	}

}

object HashStoreImpl {
	private val logger = LoggerFactory.getLogger("datastore")
}