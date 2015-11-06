package org.lipicalabs.lipica.core.trie

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, Value}
import org.slf4j.LoggerFactory

class Cache(_dataSource: KeyValueDataSource) {
	import Cache._
	import scala.collection.JavaConversions._

	private val nodes = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, CachedNode])

	def getNodes: Map[ImmutableBytes, CachedNode] = this.nodes.toMap

	private val dataSourceRef = new AtomicReference(_dataSource)
	def dataSource: KeyValueDataSource = this.dataSourceRef.get

	private val isDirtyRef = new AtomicBoolean(false)
	def isDirty: Boolean = this.isDirtyRef.get
	def setDirty(value: Boolean): Unit = {
		this.isDirtyRef.set(value)
	}

	private[trie] def privatePut(key: ImmutableBytes, value: CachedNode): Unit = {
		this.nodes.put(key, value)
	}

	def put(key: ImmutableBytes, value: Value): Unit = this.nodes.put(key, new CachedNode(value, _dirty = false))

	/**
	 * 渡されたオブジェクトのエンコードされた表現が、
	 * 32バイト（＝256ビット）よりも長ければ、キャッシュに保存します。
	 */
	def put(value: Value): Either[Value, ImmutableBytes] = {
		val encoded = value.encodedBytes
		if (32 <= encoded.length) {
			val hash = value.hash
			if (logger.isTraceEnabled) {
				logger.trace("<Cache> Putting: %s (%s): %s".format(encoded.toHexString, hash.toHexString, value))
			}
			this.nodes.put(hash, new CachedNode(value, _dirty = true))
			this.setDirty(true)
			Right(hash)
		} else {
			Left(value)
		}
	}

	def get(key: ImmutableBytes): CachedNode = {
		this.nodes.get(key) match {
			case Some(node) =>
				//キャッシュされている。
				node
			case _ =>
				//キャッシュされていないのでデータソースから読み取る。
				val data =
					if (!existsDataSource) {
						ImmutableBytes.empty
					} else {
						this.dataSource.get(key).getOrElse(ImmutableBytes.empty)
					}
				val node = new CachedNode(Value.fromEncodedBytes(data), _dirty = false)
				if (logger.isTraceEnabled) {
					logger.trace("<Cache> Read: %s -> %s -> %s".format(key, data.toHexString, node.nodeValue.asObj))
				}
				this.nodes.put(key, node)
				node
		}
	}

	def delete(key: ImmutableBytes): Unit = {
		this.nodes.remove(key)

		Option(this.dataSource).foreach {
			_.delete(key)
		}
	}

	def commit(): Unit = {
		if (!existsDataSource || !this.isDirty) return
		val start = System.nanoTime

		var totalBytes = 0
		val batch = this.nodes.entrySet().withFilter(pair => pair.getValue.isDirty).map {
			entry => {
				val nodeKey = entry.getKey
				val node = entry.getValue
				node.isDirty(false)

				val value = node.nodeValue.encodedBytes

				totalBytes += (nodeKey.length + value.length)

				if (logger.isTraceEnabled) {
					logger.trace("<Cache> Committing: %s -> %s -> %s".format(nodeKey, value.toHexString, node.nodeValue.asObj))
				}
				nodeKey -> value
			}
		}.toMap
		//保存する。
		this.dataSource.updateBatch(batch)
		this.setDirty(false)
		this.nodes.clear()

		val finish = System.nanoTime
		logger.info("<Cache> Flushed '%s' in: %,d nanos, %d nodes, %02.2fMB".format(dataSource.getName, finish - start, batch.size, totalBytes.toDouble / 1048576))
	}

	def undo(): Unit = {
		val dirtyKeys = this.nodes.entrySet().withFilter(entry => entry.getValue.isDirty).map(entry => entry.getKey)
		dirtyKeys.foreach {
			eachKey => this.nodes.remove(eachKey)
		}
		this.setDirty(false)
	}

	def setDB(aDataSource: KeyValueDataSource): Unit = {
		if (this.dataSource eq aDataSource) {
			return
		}
		val rows: Set[(ImmutableBytes, ImmutableBytes)] =
			if (!existsDataSource) {
				this.nodes.entrySet().withFilter(entry => !entry.getValue.isDirty).map {
					entry => {
						entry.getKey -> entry.getValue.nodeValue.encodedBytes
					}
				}.toSet
			} else {
				try {
					this.dataSource.keys.map(eachKey => eachKey -> this.dataSource.get(eachKey).get)
				} finally {
					this.dataSource.close()
				}
			}
		aDataSource.updateBatch(rows.toMap)
		this.dataSourceRef.set(aDataSource)
	}

	private def existsDataSource: Boolean = {
		this.dataSource ne null
	}

}

object Cache {
	private val logger = LoggerFactory.getLogger(getClass)
}