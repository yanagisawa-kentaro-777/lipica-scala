package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSourceFactory
import org.lipicalabs.lipica.core.kernel.{Address160, Address, ContractDetailsImpl, ContractDetails}
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
	private val cache = new mutable.HashMap[Address, ContractDetails]
	private val removes = new mutable.HashSet[Address]
	
	def get(key: Address): Option[ContractDetails] = {
		this.synchronized {
			this.cache.get(key) match {
				case Some(details) =>
					Some(details)
				case _ =>
					if (this.removes.contains(key)) {
						return None
					}
					this.db.get(key.bytes) match {
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

	def update(key: Address, contractDetails: ContractDetails): Unit = {
		this.synchronized {
			contractDetails.address = key
			cache.put(key, contractDetails)
			removes.remove(key)
		}
	}

	def remove(key: Address): Unit = {
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
			batch.put(key.bytes, value)
			totalSize += value.length
		}
		db.updateBatch(batch.toMap)

		//削除対象を削除する。
		for (key <- removes) {
			db.delete(key.bytes)
		}

		cache.clear()
		removes.clear()

		totalSize
	}

	def keys: Set[Address] = {
		this.synchronized {
			(this.cache.keySet ++ this.db.sortedKeys.map(each => Address160(each)).toSet).toSet
		}
	}

}

object ContractDetailsStore {
	private val logger = LoggerFactory.getLogger("datastore")
}