package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:17
 * YANAGISAWA, Kentaro
 */
class SyncQueue {
	//TODO 未実装。
	def addNewBlockHashes(hashes: Seq[ImmutableBytes]): Unit = ???
	def returnHashes(hashes: Iterable[ImmutableBytes]): Unit = ???
	def hashStoreSize: Int = ???

	def addHashesLast(hashes: Seq[ImmutableBytes]): Unit = ???

	def addBlocks(blocks: Seq[Block], nodeId: ImmutableBytes): Unit = ???
	def addNewBlock(block: Block, nodeId: ImmutableBytes): Unit = ???

	def pollHashes: Iterable[ImmutableBytes] = ???

	def logHashQueueSize(): Unit = ???

}
