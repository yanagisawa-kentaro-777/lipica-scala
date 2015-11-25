package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:20
 * YANAGISAWA, Kentaro
 */
trait HashStore extends DiskStore {

	def add(hash: ImmutableBytes): Unit

	def addFirst(hash: ImmutableBytes): Unit

	def addBatch(hashes: Seq[ImmutableBytes]): Unit

	def addBatchFirst(hashes: Seq[ImmutableBytes]): Unit

	def peek: ImmutableBytes

	def poll: ImmutableBytes

	def pollBatch(count: Int): Seq[ImmutableBytes]

	def size: Int

	def isEmpty: Boolean

	def nonEmpty: Boolean

	def keys: Set[Long]

	def clear(): Unit

	def removeAll(hashes: Iterable[ImmutableBytes]): Unit

}
