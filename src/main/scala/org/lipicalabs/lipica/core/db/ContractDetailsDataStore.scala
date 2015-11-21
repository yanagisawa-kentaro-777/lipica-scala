package org.lipicalabs.lipica.core.db

import java.util.concurrent.ConcurrentHashMap

import org.lipicalabs.lipica.core.base.{ContractDetailsImpl, ContractDetails}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class ContractDetailsDataStore {
	import ContractDetailsDataStore._
	import scala.collection.JavaConversions._

	private var _db: DatabaseImpl = null
	private val cache = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, ContractDetails])
	private val removes = new mutable.HashSet[ImmutableBytes]

	def db_=(value: DatabaseImpl): Unit = {
		this._db = value
	}
	def db: DatabaseImpl = this._db

	def get(key: ImmutableBytes): Option[ContractDetails] = {
		this.cache.get(key) match {
			case Some(details) =>
				Some(details)
			case _ =>
				if (this.removes.contains(key)) {
					return None
				}
				this.db.get(key) match {
					case Some(data) =>
						val details = ContractDetailsImpl.decode(data)
						this.cache.put(key, details)
						Some(details)
					case _ =>
						None
				}
		}
	}

	def update(key: ImmutableBytes, contractDetails: ContractDetails): Unit = {
		contractDetails.address = key
		cache.put(key, contractDetails)
		removes.remove(key)
	}

	def remove(key: ImmutableBytes): Unit = {
		this.cache.remove(key)
		this.removes.add(key)
	}

	def flush(): Unit = {
		val numberOfKeys = cache.size

		val start = System.nanoTime
		val totalSize = flushInternal
		val finish = System.nanoTime

		val flushSize = totalSize.toDouble / 1048576
		val flushMillis = (finish - start).toDouble / 1000000
		gLogger.info("Flush details in: %02.2f ms, %d keys, %02.2fMB".format(flushMillis, numberOfKeys, flushSize))
	}

	private def flushInternal: Long = {
		var totalSize = 0L

		//キャッシュに保持されている情報を、DBに永続化する。
		val batch = new mutable.HashMap[ImmutableBytes, ImmutableBytes]
		for (entry <- cache.entrySet) {
			val details = entry.getValue
			details.syncStorage()
			val key = entry.getKey
			val value = details.encode
			batch.put(key, value)
			totalSize += value.length
		}
		db.getDB.updateBatch(batch.toMap)

		//削除対象を削除する。
		for (key <- removes) {
			db.delete(key)
		}

		cache.clear()
		removes.clear()

		totalSize
	}

	def keys: Set[ImmutableBytes] = {
		(this.cache.keySet ++ this.db.sortedKeys.toSet).toSet
	}

}

object ContractDetailsDataStore {
	private val gLogger = LoggerFactory.getLogger("general")
}