package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSourceFactory
import org.lipicalabs.lipica.core.kernel.{ContractDetailsImpl, ContractDetails}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class ContractDetailsStore(private val db: DatabaseImpl, private val dataSourceFactory: KeyValueDataSourceFactory) {
	import ContractDetailsStore._

	//このインスタンス自体によってガードされている。
	private val cache = new mutable.HashMap[ImmutableBytes, ContractDetails]
	private val removes = new mutable.HashSet[ImmutableBytes]
	
	def get(key: ImmutableBytes): Option[ContractDetails] = {
		this.synchronized {
			this.cache.get(key) match {
				case Some(details) =>
					Some(details)
				case _ =>
					if (this.removes.contains(key)) {
						return None
					}
					this.db.get(key) match {
						case Some(data) =>
							val details = ContractDetailsImpl.decode(data, dataSourceFactory)
							this.cache.put(key, details)
							Some(details)
						case _ =>
							None
					}
			}
		}
	}

	def update(key: ImmutableBytes, contractDetails: ContractDetails): Unit = {
		this.synchronized {
			contractDetails.address = key
			cache.put(key, contractDetails)
			removes.remove(key)
		}
	}

	def remove(key: ImmutableBytes): Unit = {
		this.synchronized {
			this.cache.remove(key)
			this.removes.add(key)
		}
	}

	def flush(): Unit = {
		this.synchronized {
			val numberOfKeys = cache.size

			val start = System.nanoTime
			val totalSize = flushInternal
			val finish = System.nanoTime

			val flushSize = totalSize.toDouble / 1048576
			val flushMillis = (finish - start).toDouble / 1000000
			logger.info("<ContractDetailsStore> Flushed details in: %02.2f ms, %d keys, %02.2fMB".format(flushMillis, numberOfKeys, flushSize))
		}
	}

	private def flushInternal: Long = {
		var totalSize = 0L

		//キャッシュに保持されている情報を、DBに永続化する。
		val batch = new mutable.HashMap[ImmutableBytes, ImmutableBytes]
		for (entry <- cache) {
			val key = entry._1
			val details = entry._2
			details.syncStorage()
			val value = details.encode
			batch.put(key, value)
			totalSize += value.length
		}
		db.updateBatch(batch.toMap)

		//削除対象を削除する。
		for (key <- removes) {
			db.delete(key)
		}

		cache.clear()
		removes.clear()

		totalSize
	}

	def keys: Set[ImmutableBytes] = {
		this.synchronized {
			(this.cache.keySet ++ this.db.sortedKeys.toSet).toSet
		}
	}

}

object ContractDetailsStore {
	private val logger = LoggerFactory.getLogger("datastore")
}