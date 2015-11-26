package org.lipicalabs.lipica.core.db

import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.datasource.mapdb.{Serializers, MapDBFactory}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.{Serializer, DB}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/26 19:33
 * YANAGISAWA, Kentaro
 */
class HashStoreImpl(private val mapDBFactory: MapDBFactory) extends HashStore {

	import HashStoreImpl._

	private var db: DB = null
	private var hashes: mutable.Map[Long, ImmutableBytes] = null
	private var index: mutable.Buffer[Long] = null

	private var initDone: Boolean = false
	private val initLock = new ReentrantLock
	private val init = this.initLock.newCondition

	override def open(): Unit = {
		new Thread {
			override def run(): Unit = {
				initLock.lock()
				try {
					db = mapDBFactory.createTransactionalDB(dbName)
					hashes = JavaConversions.mapAsScalaMap(db.hashMapCreate(StoreName).keySerializer(Serializer.LONG).valueSerializer(Serializers.ImmutableBytes).makeOrGet())
					index = new ArrayBuffer[Long]()
					index.appendAll(hashes.keys)
					sortIndex()
					if (SystemProperties.CONFIG.databaseReset) {
						hashes.clear()
						db.commit()
					}
					initDone = true
					init.signalAll()
					logger.info("<HashStore> Hash store loaded, size[%d]".format(size))
				} finally {
					initLock.unlock()
				}
			}
		}.start()
	}

	override def close() = {
		awaitInit()
		this.db.close()
		this.initDone = false
	}

	override def add(hash: ImmutableBytes) = {
		awaitInit()
		addInternal(first = false, hash)
		this.db.commit()
	}

	override def addFirst(hash: ImmutableBytes) = {
		awaitInit()
		addInternal(first = true, hash)
		this.db.commit()
	}

	override def addBatch(aHashes: Seq[ImmutableBytes]) = {
		awaitInit()
		for (hash <- aHashes) {
			addInternal(first = false, hash)
		}
		this.db.commit()
	}

	override def addBatchFirst(aHashes: Seq[ImmutableBytes]) = {
		awaitInit()
		for (hash <- aHashes) {
			addInternal(first = true, hash)
		}
		this.db.commit()
	}

	override def peek: Option[ImmutableBytes] = {
		awaitInit()
		this.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			this.hashes.get(this.index.head)
		}
	}

	override def poll: Option[ImmutableBytes] = {
		awaitInit()
		val result = pollInternal
		this.db.commit()
		result
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

		this.db.commit()
		result.toSeq
	}

	override def isEmpty = {
		awaitInit()
		this.index.isEmpty
	}

	override def nonEmpty = !this.isEmpty

	override def keys = {
		awaitInit()
		this.hashes.keys.toSet
	}

	override def size = {
		awaitInit()
		this.index.size
	}

	override def removeAll(removing: Iterable[ImmutableBytes]) = {
		awaitInit()
		this.synchronized {
			val targets = removing.toSet
			val removed = this.hashes.withFilter(entry => targets.contains(entry._2)).map(entry => entry._1).toSet

			this.index = this.index.filter(each => !removed.contains(each))
			for (idx <- removed) {
				this.hashes.remove(idx)
			}
		}
		this.db.commit()
	}

	override def clear() = {
		awaitInit()
		this.synchronized {
			this.index.clear()
			this.hashes.clear()
		}
		this.db.commit()
	}


	private def dbName: String = "%s/%s".format(StoreName, StoreName)

	private def sortIndex(): Unit = {
		this.index = this.index.sorted
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
			this.hashes.put(idx, hash)
		}
	}

	private def pollInternal: Option[ImmutableBytes] = {
		this.synchronized {
			if (this.index.isEmpty) {
				return None
			}
			val idx = this.index.head
			val result = this.hashes.get(idx)
			this.hashes.remove(idx)
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
	private val logger = LoggerFactory.getLogger("blockqueue")
	private val StoreName: String = "hashstore"
}