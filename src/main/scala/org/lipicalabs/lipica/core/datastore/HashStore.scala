package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.utils.{DigestValue, ImmutableBytes}

/**
 * ブロックのダイジェスト値を永続化して記録するクラスが実装すべき trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:20
 * YANAGISAWA, Kentaro
 */
trait HashStore extends DiskStore {

	def add(hash: DigestValue): Unit

	def addFirst(hash: DigestValue): Unit

	def addBatch(hashes: Seq[DigestValue]): Unit

	def addBatchFirst(hashes: Seq[DigestValue]): Unit

	def peek: Option[DigestValue]

	def poll: Option[DigestValue]

	def pollBatch(count: Int): Seq[DigestValue]

	def size: Int

	def isEmpty: Boolean

	def nonEmpty: Boolean

	def keys: Set[Long]

	def clear(): Unit

}
