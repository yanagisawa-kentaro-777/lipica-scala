package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.base._
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.datasource.mapdb.MapDBFactoryImpl
import org.lipicalabs.lipica.core.db.{BlockQueueImpl, HashStoreImpl, BlockQueue, HashStore}
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.BlockHeaderValidator
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:17
 * YANAGISAWA, Kentaro
 */
class SyncQueue {
	import SyncQueue._

	private var hashStore: HashStore = null
	private var blockQueue: BlockQueue = null

	private def worldManager: WorldManager = WorldManager.instance
	private def blockchain: Blockchain = worldManager.blockchain
	private def syncManager: SyncManager = worldManager.syncManager
	private def headerValidator: BlockHeaderValidator = worldManager.blockHeaderValidator

	def init(): Unit = {
		logger.info("<SyncQueue> Start loading sync queue.")
		val mapDBFactory = new MapDBFactoryImpl
		this.hashStore = new HashStoreImpl(mapDBFactory)
		this.hashStore.open()

		this.blockQueue = new BlockQueueImpl(mapDBFactory)
		this.blockQueue.open()

		val queueProducer = new Runnable {
			override def run() = produceQueue()
		}
		new Thread(queueProducer).start()
	}

	private def produceQueue(): Unit = {
		while (true) {
			try {
				val blockWrapper = this.blockQueue.take
				logger.info("<SyncQueue> BlockQueue size=%,d".format(this.blockQueue.size))
				val importResult = this.blockchain.tryToConnect(blockWrapper.block)

				//if ((blockWrapper.blockNumber % 100) == 0) {
					//println("Block[%,d] %s".format(blockWrapper.blockNumber, importResult))
				//}

				if (importResult == ImportResult.NoParent) {
					logger.info("<SyncQueue> No parent on the chain for BlockNumber %,d & BlockHash %s".format(blockWrapper.blockNumber, blockWrapper.block.shortHash))
					blockWrapper.fireImportFailed()
					this.syncManager.tryGapRecovery(blockWrapper)
					this.blockQueue.add(blockWrapper)
					Thread.sleep(2000L)
				}
				if (blockWrapper.isNewBlock && importResult.isSuccessful) {
					this.syncManager.notifyNewBlockImported(blockWrapper)
				}
				if (importResult == ImportResult.ImportedBest) {
					logger.info("<SyncQueue> Success importing BEST: BlockNumber=%,d & BlockHash=%s, Tx.size=%,d".format(blockWrapper.blockNumber, blockWrapper.block.shortHash, blockWrapper.block.transactions.size))
				}
				if (importResult == ImportResult.ImportedNotBest) {
					logger.info("<SyncQueue> Success importing NOT_BEST: BlockNumber=%,d & BlockHash=%s, Tx.size=%,d".format(blockWrapper.blockNumber, blockWrapper.block.shortHash, blockWrapper.block.transactions.size))
				}
			} catch {
				case e: Throwable =>
					logger.warn("<SyncQueue> Exception caught: %s".format(e.getClass.getSimpleName), e)
			}
		}
	}

	def addBlocks(blocks: Seq[Block], nodeId: ImmutableBytes): Unit = {
		if (blocks.isEmpty) {
			return
		}
		blocks.find(each => !isValid(each.blockHeader)).foreach {
			found => {
				if (logger.isDebugEnabled) {
					logger.debug("<SyncQueue> Invalid block: %s".format(found.toString))
				}
				syncManager.reportInvalidBlock(nodeId)
				return
			}
		}
		if (logger.isDebugEnabled) {
			logger.debug("<SyncQueue> Adding %,d blocks from [%,d  %s]".format(blocks.size, blocks.head.blockNumber, blocks.head.hash))
		}
		val wrappers = blocks.map(each => BlockWrapper(each, nodeId))
		this.blockQueue.addAll(wrappers)
		if (logger.isDebugEnabled) {
			logger.debug("<SyncQueue> Blocks waiting to be processed: Queue.size=%,d LastBlock.Number=%,d".format(this.blockQueue.size, blocks.last.blockNumber))
		}
	}

	def addNewBlock(block: Block, nodeId: ImmutableBytes): Unit = {
		if (!isValid(block.blockHeader)) {
			this.syncManager.reportInvalidBlock(nodeId)
			return
		}
		val wrapper = BlockWrapper(block, newBlock = true, nodeId = nodeId)
		wrapper.receivedAt = System.currentTimeMillis
		this.blockQueue.add(wrapper)

		if (logger.isDebugEnabled) {
			logger.debug("<SyncQueue> Blocks waiting to be processed: Queue.size=%,d LastBlock.Number=%,d".format(this.blockQueue.size, block.blockNumber))
		}
	}

	def addHash(hash: ImmutableBytes): Unit = {
		this.hashStore.addFirst(hash)
		if (logger.isTraceEnabled) {
			logger.trace("<SyncQueue> Adding hash to a hashQueue: %s, hashQueue.size=%,d".format(hash.toShortString, this.hashStore.size))
		}
	}

	def addHashesLast(hashes: Seq[ImmutableBytes]): Unit = {
		val filtered = this.blockQueue.filterExisting(hashes)
		this.hashStore.addBatch(filtered)
		if (logger.isDebugEnabled) {
			logger.debug("<SyncQueue> %,d hashes filtered out, %,d added.".format(hashes.size - filtered.size, filtered.size))
		}
	}

	def addHashes(hashes: Seq[ImmutableBytes]): Unit = {
		val filtered = this.blockQueue.filterExisting(hashes)
		this.hashStore.addBatchFirst(filtered)
		if (logger.isDebugEnabled) {
			logger.debug("<SyncQueue> %,d hashes filtered out, %,d added.".format(hashes.size - filtered.size, filtered.size))
		}
	}

	def addNewBlockHashes(hashes: Seq[ImmutableBytes]): Unit = {
		val notInQueue = this.blockQueue.filterExisting(hashes)
		val notInChain = notInQueue.filter(each => !this.blockchain.existsBlock(each))
		this.hashStore.addBatch(notInChain)
	}


	def returnHashes(hashes: Seq[ImmutableBytes]): Unit = {
		if (hashes.isEmpty) {
			return
		}
		logger.info("<SyncQueue> Hashes remained uncovered: hashes.size=%,d".format(hashes.size))
		hashes.reverse.foreach {
			each => {
				if (logger.isDebugEnabled) {
					logger.debug("<SyncQueue> Putting hash: %s".format(each))
				}
				this.hashStore.addFirst(each)
			}
		}
	}

	def pollHashes: Seq[ImmutableBytes] = this.hashStore.pollBatch(SystemProperties.CONFIG.maxBlocksAsk)

	def logHashQueueSize(): Unit = logger.info("<SyncQueue> Block hashes list size = %,d".format(this.hashStore.size))

	def size: Int = this.blockQueue.size

	def clear(): Unit = {
		this.hashStore.clear()
		this.blockQueue.clear()
	}

	def hashStoreSize: Int = this.hashStore.size

	def isHashesEmpty: Boolean = this.hashStore.isEmpty

	def isBlocksEmpty: Boolean = this.blockQueue.isEmpty

	def clearHashes(): Unit = this.hashStore.clear()

	def hasSolidBlocks: Boolean = this.blockQueue.peek.exists(_.isSolidBlock)

	private def isValid(header: BlockHeader): Boolean = {
		if (!this.headerValidator.validate(header)) {
			if (logger.isWarnEnabled) {
				this.headerValidator.logErrors(logger)
			}
			false
		} else {
			true
		}
	}

}

object SyncQueue {
	private val logger = LoggerFactory.getLogger("blockqueue")
}