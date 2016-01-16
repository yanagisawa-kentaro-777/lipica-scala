package org.lipicalabs.lipica.core.trie

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * Trieのバックエンドとして動作する永続化機構を表すクラスです。
 */
class TrieBackend(_dataSource: KeyValueDataSource) {
	import TrieBackend._
	import scala.collection.JavaConversions._

	private val encodedNodes = mapAsScalaConcurrentMap(new ConcurrentHashMap[DigestValue, EncodedTrieEntry])

	def entries: Map[DigestValue, EncodedTrieEntry] = this.encodedNodes.toMap

	private val dataSourceRef = new AtomicReference(_dataSource)
	def dataSource: KeyValueDataSource = this.dataSourceRef.get

	private val isDirtyRef = new AtomicBoolean(false)
	def isDirty: Boolean = this.isDirtyRef.get
	def isDirty_=(value: Boolean): Unit = {
		this.isDirtyRef.set(value)
	}

	private[trie] def privatePut(key: DigestValue, value: EncodedTrieEntry): Unit = {
		this.encodedNodes.put(key, value)
	}

	def put(key: DigestValue, bytes: ImmutableBytes): Unit = this.encodedNodes.put(key, new EncodedTrieEntry(bytes, _dirty = false))

	/**
	 * 渡されたオブジェクトのエンコードされた表現が、
	 * 32バイト（＝256ビット）よりも長ければ、キャッシュに保存します。
	 */
	def put(trieNode: TrieNode): Either[TrieNode, DigestValue] = {
		val encoded = trieNode.toEncodedBytes
		if (32 <= encoded.length) {
			val hash = trieNode.hash
			if (logger.isTraceEnabled) {
				logger.trace("<Cache> Putting: %s (%s): %s".format(encoded.toHexString, hash.toHexString, trieNode))
			}
			this.encodedNodes.put(hash, new EncodedTrieEntry(encoded, _dirty = true))
			this.isDirty = true
			Right(hash)
		} else {
			Left(trieNode)
		}
	}

	def get(key: DigestValue): EncodedTrieEntry = {
		this.encodedNodes.get(key) match {
			case Some(node) =>
				//キャッシュされている。
				node
			case _ =>
				//キャッシュされていないのでデータソースから読み取る。
				val data =
					if (!existsDataSource) {
						ImmutableBytes.empty
					} else {
						this.dataSource.get(key.bytes).getOrElse(ImmutableBytes.empty)
					}
				val node = new EncodedTrieEntry(data, _dirty = false)
				if (logger.isTraceEnabled) {
					logger.trace("<Cache> Read: %s -> %s -> %s".format(key, data.toHexString, node.encodedBytes))
				}
				this.encodedNodes.put(key, node)
				node
		}
	}

	def delete(key: ImmutableBytes): Unit = {
		this.encodedNodes.remove(key)

		Option(this.dataSource).foreach {
			_.delete(key)
		}
	}

	def commit(): Unit = {
		if (!existsDataSource || !this.isDirty) return
		val start = System.nanoTime

		var totalBytes = 0
		val batch = this.encodedNodes.entrySet().withFilter(pair => pair.getValue.isDirty).map {
			entry => {
				val nodeKey = entry.getKey
				val node = entry.getValue
				node.isDirty(false)

				val value = node.encodedBytes

				totalBytes += (nodeKey.length + value.length)

				if (logger.isTraceEnabled) {
					logger.trace("<Cache> Committing: %s -> %s -> %s".format(nodeKey, value.toHexString, node.encodedBytes))
				}
				nodeKey.bytes -> value
			}
		}.toMap
		//保存する。
		this.dataSource.updateBatch(batch)
		this.isDirty = false
		this.encodedNodes.clear()

		val finish = System.nanoTime
		logger.info("<Cache> Flushed '%s' in: %,d nanos, %d nodes, %02.2fMB".format(dataSource.name, finish - start, batch.size, totalBytes.toDouble / 1048576))
	}

	def undo(): Unit = {
		val dirtyKeys = this.encodedNodes.entrySet().withFilter(entry => entry.getValue.isDirty).map(entry => entry.getKey)
		dirtyKeys.foreach {
			eachKey => this.encodedNodes.remove(eachKey)
		}
		this.isDirty = false
	}

	def assignDataSource(aDataSource: KeyValueDataSource): Unit = {
		if (this.dataSource eq aDataSource) {
			return
		}
		val rows: Set[(ImmutableBytes, ImmutableBytes)] =
			if (!existsDataSource) {
				this.encodedNodes.entrySet().withFilter(entry => !entry.getValue.isDirty).map {
					entry => {
						entry.getKey.bytes -> entry.getValue.encodedBytes
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

object TrieBackend {
	private val logger = LoggerFactory.getLogger("trie")
}

/**
 * Trieにおける１ノードをエンコードした表現を
 * 永続化用に保持するためのクラスです。
 */
class EncodedTrieEntry(val encodedBytes: ImmutableBytes, _dirty: Boolean) {
	/**
	 * 永続化されていない更新があるか否か。
	 */
	private val isDirtyRef = new AtomicBoolean(_dirty)

	def isDirty = this.isDirtyRef.get

	def isDirty(value: Boolean): EncodedTrieEntry = {
		this.isDirtyRef.set(value)
		this
	}
}
