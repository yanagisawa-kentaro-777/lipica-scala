package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.BlockWrapper

/**
 * 同期対象とするブロックを蓄積するキューです。
 *
 * Created by IntelliJ IDEA.
 * @author YANAGISAWA, Kentaro
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

	/**
	 * 渡されたハッシュ値の中から、既にこのキューに溜まっているものを除外したものを返します。
	 */
	def excludeExisting(hashes: Seq[DigestValue]): Seq[DigestValue]

}
