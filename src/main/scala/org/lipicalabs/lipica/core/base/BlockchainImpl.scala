package org.lipicalabs.lipica.core.base

import java.io.{Closeable, BufferedWriter, FileWriter}

import org.lipicalabs.lipica.core.ImportResult
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db.{Repository, BlockStore}
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.manager.AdminInfo
import org.lipicalabs.lipica.core.trie.TrieImpl
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes, UtilConsts}
import org.lipicalabs.lipica.core.validator.ParentBlockHeaderValidator
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeFactory
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * validationのアルゴリズム。
 * ・直前のブロックとして参照されたものが存在するか。そして有効か。
 * ・当該ブロックのタイムスタンプが、直前のブロックのタイムスタンプよりも大きく、そして15分未満であるか。
 * ・ブロック番号、難度、トランザクションルート、アンクルルート、mana limitが正しいか。
 * ・PoWが有効か。
 * ・前ブロックのStateRootの状態に対し、当該ブロック内のすべてのトランザクションを適用し、採掘報酬を支払った結果の
 * StateRootが正しいこと。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:35
 * YANAGISAWA, Kentaro
 */
class BlockchainImpl(
	private val blockStore: BlockStore,
	private var _repository: Repository,
	private val wallet: Wallet,
	private val adminInfo: AdminInfo,
	private val listener: LipicaListener,
	private val parentHeaderValidator: ParentBlockHeaderValidator
) extends BlockChain {

	import BlockchainImpl._

	private val _pendingTransactions = new mutable.HashSet[PendingTransaction]

	private var track: Repository = null

	private var _bestBlock: Block = null
	override def bestBlock: Block = this._bestBlock
	override def bestBlock_=(block: Block): Unit = {
		this._bestBlock = block
	}

	private var _totalDifficulty: BigInt = UtilConsts.Zero
	override def totalDifficulty: BigInt = this._totalDifficulty
	override def totalDifficulty_=(v: BigInt): Unit = this._totalDifficulty = v

	private val _altChains: mutable.Buffer[Chain] = new ArrayBuffer[Chain]
	override def altChains: Iterable[Chain] = this._altChains.toIterable

	private val _garbage: mutable.Buffer[Block] = new ArrayBuffer[Block]
	override def garbage: Iterable[Block] = this._garbage.toIterable

	def repository: Repository = this._repository
	def repository_=(v: Repository): Unit = this._repository = v

	private var _programInvokeFactory: ProgramInvokeFactory = null
	def programInvokeFactory_=(v: ProgramInvokeFactory): Unit = this._programInvokeFactory = v

	private var _exitOn = Long.MaxValue
	override def exitOn: Long = this._exitOn
	override def exitOn_=(v: Long): Unit = this._exitOn = v

	var byTest: Boolean = false
	private var fork: Boolean = false

	/**
	 * このブロックチェーンにおける最新ブロックのダイジェスト値を返します。
	 */
	override def bestBlockHash: ImmutableBytes = bestBlock.hash

	/**
	 * このブロックチェーンに登録されているブロック数を返します。
	 */
	override def size = this.bestBlock.blockNumber + 1

	/**
	 * ブロック番号に対応するブロックを返します。
	 */
	override def getBlockByNumber(blockNumber: Long) = this.blockStore.getChainBlockByNumber(blockNumber)

	/**
	 * ダイジェスト値に対応するブロックを返します。
	 */
	override def getBlockByHash(hash: ImmutableBytes) = this.blockStore.getBlockByHash(hash)

	override def getTransactionReceiptByHash(hash: ImmutableBytes) = {
		throw new UnsupportedOperationException("Not implemented.")
	}


	override def getSeqOfHashesStartingFrom(hash: ImmutableBytes, count: Int): Seq[ImmutableBytes] = {
		//逆順でよい。
		this.blockStore.getHashesEndingWith(hash, count)
	}

	override def getSeqOfHashesStartingFromBlock(aBlockNumber: Long, aCount: Int): Seq[ImmutableBytes] = {
		val bestBlockNumber = this.bestBlock.blockNumber
		if (bestBlockNumber < aBlockNumber) {
			return Seq.empty
		}
		val count = aCount min (bestBlockNumber - aBlockNumber + 1).toInt
		val endBlockNumber = aBlockNumber + count - 1
		val block = getBlockByNumber(endBlockNumber).get
		val hashes = this.blockStore.getHashesEndingWith(block.hash, count)
		hashes.reverse
	}

	/**
	 * 渡されたブロックを、このチェーンに連結しようと試みます。
	 */
	override def tryToConnect(block: Block): ImportResult = {
		logger.info("<BlockchainImpl> Trying to connect block: Hash=%s, BlockNumber=%,d".format(block.hash, block.blockNumber))
		if ((block.blockNumber <= this.blockStore.getMaxBlockNumber) && this.blockStore.existsBlock(block.hash)) {
			//既存ブロック。
			if (logger.isDebugEnabled) {
				logger.debug("<BlockchainImpl> Block already exists: Hash=%s, BlockNumber=%d".format(block.hash, block.blockNumber))
			}
			ImportResult.Exists
		} else if (this.bestBlock.isParentOf(block)) {
			//単純連結。
			recordBlock(block)
			append(block)
			ImportResult.ImportedBest
		} else if (this.blockStore.existsBlock(block.parentHash)) {
			//フォーク連結。
			recordBlock(block)
			tryConnectAndFork(block)
		} else {
			//得体の知れないブロック。
			ImportResult.NoParent
		}
	}

	def tryConnectAndFork(block: Block): ImportResult = {
		val savedRepo = this.repository
		val savedBest = this.bestBlock
		val savedTD = this.totalDifficulty

		this.bestBlock = this.blockStore.getBlockByHash(block.parentHash).get
		this.totalDifficulty = this.blockStore.getTotalDifficultyForHash(block.parentHash)
		this.repository = this.repository.getSnapshotTo(this.bestBlock.stateRoot)
		this.fork = true
		try {
			append(block)
		} finally {
			this.fork = false
		}

		if (savedTD < this.totalDifficulty) {
			//こちらのほうがdifficultyが高いので、rebranchする！
			logger.info("<BlockchainImpl> Rebranching: %s -> %s".format(savedBest.shortHash, block.shortHash))
			this.blockStore.rebranch(block)
			this.repository = savedRepo
			this.repository.syncToRoot(block.stateRoot)
			if (!byTest) {
				this.repository.flush()
				this.blockStore.flush()
				System.gc()
			}
			ImportResult.ImportedBest
		} else {
			this.repository = savedRepo
			this.bestBlock = savedBest
			this.totalDifficulty = savedTD

			ImportResult.ImportedNotBest
		}
	}

	private def recordBlock(block: Block): Unit = {
		if (!SystemProperties.CONFIG.recordBlocks) {
			return
		}
		val dumpDir = SystemProperties.CONFIG.databaseDir + "/" + SystemProperties.CONFIG.dumpDir
		val dumpFile = new java.io.File(dumpDir + "/blocks-rec.dmp")
		var fw: FileWriter = null
		var bw: BufferedWriter = null
		try {
			dumpFile.getParentFile.mkdirs()
			if (!dumpFile.exists()) {
				dumpFile.createNewFile()
			}
			fw = new FileWriter(dumpFile.getAbsoluteFile, true)
			bw = new BufferedWriter(fw)

			if (this.bestBlock.isGenesis) {
				bw.write(this.bestBlock.encode.toHexString)
				bw.write("\n")
			}
			bw.write(block.encode.toHexString)
			bw.write("\n")

			bw.flush()
			fw.flush()
		} catch {
			case e: Throwable =>
				logger.warn("<BlockchainImpl>", e)
		} finally {
			closeIfNotNull(bw)
			closeIfNotNull(fw)
		}
	}

	private def closeIfNotNull(closeable: Closeable): Unit = {
		if (closeable ne null) {
			try {
				closeable.close()
			} catch {
				case e: Throwable =>
					logger.warn("<BlockchainImpl>", e)
			}
		}
	}

	override def append(block: Block): Unit = {
		if (block eq null) {
			return
		}

		if (this.exitOn < block.blockNumber) {
			System.out.println("<BlockchainImpl> Exiting after BlockNumber: %,d".format(this.bestBlock.blockNumber))
			System.exit(-1)
		}

		//ブロック自体の破損検査を行う。
		if (!isValid(block)) {
			logger.warn("<BlockchainImpl> Invalid block: BlockNumber=%,d".format(block.blockNumber))
			return
		}
		this.track = this.repository.startTracking
		if (this.bestBlock.hash != block.parentHash) {
			return
		}
		//ブロック内のコードを実行する。
		val receipts: Seq[TransactionReceipt] = processBlock(block)
		val calculatedReceiptsHash = calculateReceiptsTrie(receipts)
		if (block.receiptsRoot != calculatedReceiptsHash) {
			logger.warn("<BlockchainImpl> Block's given receipt hash doesn't match: %s != %s".format(block.receiptsRoot, calculatedReceiptsHash))
		}
		val calculatedLogBloomHash = calculateLogBloom(receipts)
		if (block.logsBloom != calculatedLogBloomHash) {
			logger.warn("<BlockchainImpl> Block's given log bloom filter doesn't match: %s != %s".format(block.logsBloom, calculatedLogBloomHash))
		}

		track.commit()
		//ブロックを保存する。
		storeBlock(block, receipts)

		if (needsFlushing(block)) {
			this.repository.flush()
			this.blockStore.flush()
			System.gc()
		}
		//approve されたのでペンディング状態を解消する。
		this.wallet.removeTransactions(block.transactions)
		clearPendingTransactions(block.transactions)
		clearOutdatedTransactions(block.blockNumber)

		this.listener.trace("Block chain size: [%,d]".format(this.size))
		this.listener.onBlock(block, receipts)
	}

	private def processBlock(block: Block): Seq[TransactionReceipt] = {
		if (!block.isGenesis && !SystemProperties.CONFIG.blockchainOnly) {
			this.wallet.addTransactions(block.transactions)
			val result = applyBlock(block)
			this.wallet.processBlock(block)
			result
		} else {
			Seq.empty
		}
	}

	private def applyBlock(block: Block): Seq[TransactionReceipt] = {
		logger.info("<BlockchainImpl> Applying block: BlockNumber=%,d, TxSize=%,d".format(block.blockNumber, block.transactions.size))
		val startTime = System.nanoTime

		val receipts = new ArrayBuffer[TransactionReceipt]
		var totalManaUsed = 0L
		for (tx <- block.transactions) {
			val executor = new TransactionExecutor(tx, block.coinbase, this.track, this.blockStore, this._programInvokeFactory, block, this.listener, totalManaUsed)
			executor.init()
			executor.execute()
			executor.go()
			executor.finalization()

			totalManaUsed = executor.manaUsed

			this.track.commit()
			val usedManaBytes = ImmutableBytes.asUnsignedByteArray(BigInt(totalManaUsed))
			val receipt = TransactionReceipt(this.repository.getRoot, usedManaBytes, Bloom(), executor.logs)
			receipt.transaction = tx

			receipts.append(receipt)
		}
		addReward(block)
		updateTotalDifficulty(block)

		this.track.commit()
		val endTime = System.nanoTime

		this.adminInfo.addBlockExecNanos(endTime - startTime)
		receipts.toSeq
	}

	private def addReward(block: Block): Unit = {
		var totalBlockReward = Block.BlockReward
		if (0 < block.uncles.size) {
			for (uncle <- block.uncles) {
				val uncleReward = BigDecimal(Block.BlockReward) * (BigDecimal(8 + uncle.blockNumber - block.blockNumber) / BigDecimal(8))
				this.track.addBalance(uncle.coinbase, uncleReward.toBigInt())
				totalBlockReward = totalBlockReward + Block.InclusionReward
			}
		}
		this.track.addBalance(block.coinbase, totalBlockReward)
	}

	private def needsFlushing(block: Block): Boolean = {
		if (0d < SystemProperties.CONFIG.cacheFlushMemory) {
			needsFlushingByMemory(SystemProperties.CONFIG.cacheFlushMemory)
		} else if (0 < SystemProperties.CONFIG.cacheFlushBlocks) {
			(block.blockNumber % SystemProperties.CONFIG.cacheFlushBlocks) == 0
		} else {
			needsFlushingByMemory(0.7d)
		}
	}

	private def needsFlushingByMemory(rate: Double): Boolean = {
		val runtime = Runtime.getRuntime
		runtime.freeMemory() < (runtime.totalMemory() * (1 - rate))
	}

	private def calculateReceiptsTrie(receipts: Seq[TransactionReceipt]): ImmutableBytes = {
		val trie = new TrieImpl(null)
		if (receipts.isEmpty) {
			DigestUtils.EmptyTrieHash
		} else {
			for (i <- receipts.indices) {
				val key = RBACCodec.Encoder.encode(i)
				val value = receipts(i).encode
				trie.update(key, value)
			}
			trie.rootHash
		}
	}

	private def calculateLogBloom(receipts: Seq[TransactionReceipt]): ImmutableBytes = {
		var result = Bloom()
		for (receipt <- receipts) {
			result = result | receipt.bloomFilter
		}
		result.immutableBytes
	}

	private def calculateTxTrie(transactions: Seq[TransactionLike]): ImmutableBytes = {
		val trie = new TrieImpl(null)
		if (transactions.isEmpty) {
			return DigestUtils.EmptyTrieHash
		}
		for (i <- transactions.indices) {
			val key = RBACCodec.Encoder.encode(i)
			val value = transactions(i).encodedBytes
			trie.update(key, value)
		}
		trie.rootHash
	}

	def getParentOf(header: BlockHeader): Option[Block] = this.blockStore.getBlockByHash(header.parentHash)

	private def isValid(block: Block): Boolean = {
		if (block.isGenesis) {
			return true
		}
		if (!isValid(block.blockHeader)) {
			return false
		}
		if (block.txTrieRoot != calculateTxTrie(block.transactions)) {
			return false
		}
		val UncleListLimit = 2
		if (UncleListLimit < block.uncles.size) {
			return false
		}

		if (block.blockHeader.unclesHash != ImmutableBytes(DigestUtils.digest256(block.blockHeader.encodeUncles(block.uncles).toByteArray))) {
			return false
		}

		val UncleGenerationLimit = 7
		for (uncle <- block.uncles) {
			if (!isValid(uncle)) {
				return false
			}
			if (getParentOf(uncle).get.blockNumber < (block.blockNumber - UncleGenerationLimit)) {
				return false
			}
		}
		true
	}

	def isValid(header: BlockHeader): Boolean = {
		getParentOf(header) match {
			case Some(parent) =>
				if (!this.parentHeaderValidator.validate(header, parent.blockHeader)) {
					this.parentHeaderValidator.logErrors(logger)
					false
				} else {
					true
				}
			case _ =>
				false
		}
	}

	override def hasParentOnTheChain(block: Block) = getParentOf(block.blockHeader).isDefined

	override def close() = ()

	override def updateTotalDifficulty(block: Block) = {
		this._totalDifficulty += block.difficultyAsBigInt
		logger.info("<BlockChainImpl> TotalDifficulty updated to %d".format(this._totalDifficulty))
	}

	override def storeBlock(block: Block, receipts: Seq[TransactionReceipt]): Unit = {
		if (!SystemProperties.CONFIG.blockchainOnly) {
			if (block.stateRoot != this.repository.getRoot) {
				stateLogger.warn("<BlockchainImpl> State conflict! BlockNumber: %d, %s != %s".format(block.blockNumber, block.stateRoot, this.repository.getRoot))
				this.adminInfo.lostConsensus()
				System.exit(1)
			}
		}

		if (this.fork) {
			this.blockStore.saveBlock(block, this.totalDifficulty, mainChain = false)
		} else {
			this.blockStore.saveBlock(block, this.totalDifficulty, mainChain = true)
		}
		this.bestBlock = block
		logger.info("<BlockchainImpl> Block appended to the chain: Number: %d, Hash: %s, TotalDifficulty: %d".format(block.blockNumber, block.shortHash, this.totalDifficulty))
	}

	override def addPendingTransactions(transactions: Set[TransactionLike]): Unit = {
		logger.info("<BlockchainImpl> Pending tx list added: Size: %,d".format(transactions.size))
		if (transactions.isEmpty) {
			return
		}
		if (this.listener ne null) {
			this.listener.onPendingTransactionsReceived(transactions)
		}
		val bestBlockNumber = this.bestBlock.blockNumber
		for (tx <- transactions) {
			val txNonce = tx.nonce.toPositiveBigInt
			this.repository.getAccountState(tx.sendAddress) match {
				case Some(account) =>
					val currentNonce = account.nonce
					if (currentNonce == txNonce) {
						this._pendingTransactions.add(PendingTransaction(tx, bestBlockNumber))
					}
				case _ =>
					if (txNonce == UtilConsts.Zero) {
						this._pendingTransactions.add(PendingTransaction(tx, bestBlockNumber))
					}
			}
		}
	}

	override def clearPendingTransactions(receivedTransactions: Iterable[TransactionLike]) = {
		for (tx <- receivedTransactions) {
			logger.info("<BlockChainImpl> Clear tx: hash=%s".format(tx.hash.toHexString))
			this._pendingTransactions.remove(PendingTransaction(tx, 0))
		}
	}

	private def clearOutdatedTransactions(blockNumber: Long): Unit = {
		val outdated = new ArrayBuffer[PendingTransaction]
		val transactions = new ArrayBuffer[TransactionLike]
		this._pendingTransactions.synchronized {
			for (tx <- this._pendingTransactions) {
				if (SystemProperties.CONFIG.txOutdatedThreshold < blockNumber - tx.blockNumer) {
					outdated.append(tx)
					transactions.append(tx.transaction)
				}
			}
		}
		if (outdated.isEmpty) {
			return
		}
		outdated.foreach(each => this._pendingTransactions.remove(each))
		this.wallet.removeTransactions(transactions)
	}

	override def pendingTransactions: Set[TransactionLike] = {
		this._pendingTransactions.map(_.transaction).toSet
	}

	def startTracking(): Unit = {
		this.track = this.repository.startTracking
	}

	override def existsBlock(hash: ImmutableBytes) = this.blockStore.existsBlock(hash)
}

object BlockchainImpl {
	private val logger = LoggerFactory.getLogger("blockchain")
	private val stateLogger = LoggerFactory.getLogger("state")
}