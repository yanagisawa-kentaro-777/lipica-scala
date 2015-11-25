package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.BlockWrapper
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:44
 * YANAGISAWA, Kentaro
 */
trait BlockQueue extends DiskStore {

	def addAll(blocks: Iterable[BlockWrapper]): Unit

	def add(block: BlockWrapper): Unit

	def poll: Option[BlockWrapper]

	def peek: Option[BlockWrapper]

	def take: BlockWrapper

	def size: Int

	def isEmpty: Boolean

	def nonEmpty: Boolean

	def clear(): Unit

	def filterExisting(hashes: Seq[ImmutableBytes]): Seq[ImmutableBytes]

}
