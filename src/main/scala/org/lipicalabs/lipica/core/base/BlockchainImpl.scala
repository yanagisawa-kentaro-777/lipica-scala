package org.lipicalabs.lipica.core.base

import java.io.{Closeable, BufferedWriter, FileWriter}
import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.{RepositoryTrackLike, Repository, BlockStore}
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.manager.AdminInfo
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}
import org.lipicalabs.lipica.core.validator._
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeFactory
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * 自ノードが管理するブロックチェーンの実装クラスです。
 *
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
) extends Blockchain {

	import BlockchainImpl._

	private val _pendingTransactions = new mutable.HashSet[PendingTransaction]

	private var track: RepositoryTrackLike = null

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
	def programInvokeFactory: ProgramInvokeFactory = this._programInvokeFactory
	def programInvokeFactory_=(v: ProgramInvokeFactory): Unit = this._programInvokeFactory = v

	private var _exitOn = Long.MaxValue
	override def exitOn: Long = this._exitOn
	override def exitOn_=(v: Long): Unit = this._exitOn = v

	private val processingBlockRef: AtomicReference[Block] = new AtomicReference[Block](null)

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

	/**
	 * 渡されたハッシュ値を持つブロック以前のブロックのハッシュ値を並べて返します。
	 * 並び順は、最も新しい（＝ブロック番号が大きい）ブロックを先頭として過去に遡行する順序となります。
	 */
	override def getSeqOfHashesEndingWith(hash: ImmutableBytes, count: Int): Seq[ImmutableBytes] = {
		//逆順でよい。
		this.blockStore.getHashesEndingWith(hash, count)
	}

	/**
	 * 渡されたブロック番号を最古（＝最小）のブロック番号として、
	 * そこから指定された個数分だけのより新しい（＝ブロック番号が大きい）ブロックを
	 * 並べて返します。
	 * 並び順は、最も新しい（＝ブロック番号が大きい）ブロックを先頭として
	 * 過去に遡行する形となります。
	 */
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
	 *
	 * ピアとのsync時に、ピアからネットワーク越しに受領したブロックを連結する場合。
	 * ローカルに保存していたブロック情報を連結する場合。
	 */
	override def tryToConnect(block: Block): ImportResult = {
		this.processingBlockRef.set(block)
		try {
			logger.info("<Blockchain> Trying to connect %s. Repos state=%s".format(block.summaryString(short = true), this.repository.rootHash))
			val result =
				if (this.bestBlock eq null) {
					if (logger.isDebugEnabled) {
						logger.debug("<Blockchain> The first block: %s".format(block.summaryString(short = true)))
					}
					recordBlock(block)
					append(block)
					ImportResult.ImportedBest
				} else if ((block.blockNumber <= this.blockStore.getMaxBlockNumber) && this.blockStore.existsBlock(block.hash)) {
					//既存ブロック。
					if (logger.isDebugEnabled) {
						logger.debug("<Blockchain> Block already exists: %s".format(block.summaryString(short = true)))
					}
					ImportResult.Exists
				} else if (this.bestBlock.isParentOf(block)) {
					//単純連結。
					if (logger.isDebugEnabled) {
						logger.debug("<Blockchain> Appending block: %s".format(block.summaryString(short = true)))
					}
					recordBlock(block)
					append(block)
					ImportResult.ImportedBest
				} else if (this.blockStore.existsBlock(block.parentHash)) {
					//フォーク連結。
					if (logger.isDebugEnabled) {
						logger.debug("<Blockchain> Forking block: %s".format(block.summaryString(short = true)))
					}
					recordBlock(block)
					tryConnectAndFork(block)
				} else {
					//得体の知れないブロック。
					logger.info("<Blockchain> Unknown block: %s".format(block.summaryString(short = true)))
					ImportResult.NoParent
				}
			logger.info("<Blockchain> Tried to connect %s. Result=%s. Repos state=%s".format(block.summaryString(short = false), result, this.repository.rootHash))
			result
		} finally {
			this.processingBlockRef.set(null)
		}
	}

	private def tryConnectAndFork(block: Block): ImportResult = {
		val savedRepo = this.repository
		val savedBest = this.bestBlock
		val savedTD = this.totalDifficulty

		//渡されたブロックとこのチェーンとの共通祖先まで、状態を遡る。
		this.bestBlock = this.blockStore.getBlockByHash(block.parentHash).get
		this.totalDifficulty = this.blockStore.getTotalDifficultyForHash(block.parentHash)
		this.repository = this.repository.createSnapshotTo(this.bestBlock.stateRoot)
		this.fork = true
		try {
			//共通祖先に、渡されたブロックを追加する。
			append(block)
		} finally {
			this.fork = false
		}

		if (savedTD < this.totalDifficulty) {
			//こちらのチェーンの方が、total difficultyが大きいので、PoW勝負で勝ちである。
			//こちらにrebranchする！
			logger.info("<Blockchain> Rebranching: %s -> %s".format(savedBest.summaryString(short = true), block.summaryString(short = true)))
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
			//巻き戻す。ただし追加されたフォークは残る。
			logger.info("<Blockchain> No need to rebranch. Best block is still %s".format(savedBest.summaryString(short = true)))
			this.repository = savedRepo
			this.bestBlock = savedBest
			this.totalDifficulty = savedTD

			ImportResult.ImportedNotBest
		}
	}

	override def processingBlockOption: Option[Block] = Option(this.processingBlockRef.get)

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
				logger.warn("<Blockchain>", e)
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
					logger.warn("<Blockchain>", e)
			}
		}
	}

	override def append(block: Block): Unit = {
		if (this.exitOn < block.blockNumber) {
			val message = "<Blockchain> Exiting after BlockNumber: %,d".format(this.bestBlock.blockNumber)
			logger.info(message)
			System.out.println(message)
			System.exit(-1)//TODO いささか無作法に過ぎるが。
		}

		//ブロック自体の破損検査を行う。
		if (!isValid(block)) {
			logger.warn("<Blockchain> INVALID BLOCK: %s".format(block.summaryString(short = true)))
			return
		}
		if ((this.bestBlock ne null) && this.bestBlock.hash != block.parentHash) {
			logger.warn("<Blockchain> CANNOT CONNECT: %s is not the parent of %s".format(this.bestBlock.summaryString(short = true), block.summaryString(short = true)))
			return
		}

		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> Before processing %s. Current repos root: %s".format(block.summaryString(short = true), this.repository.rootHash.toShortString))
		}
		this.track = this.repository.startTracking
		//ブロック内のコードを実行する。
		val receipts = processBlock(block)
		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> Current coinbase balance: %,d".format(this.repository.getBalance(block.coinbase).getOrElse(UtilConsts.Zero)))
		}

		val calculatedReceiptsHash = TxReceiptTrieRootCalculator.calculateReceiptsTrieRoot(receipts)
		if (block.receiptsRoot != calculatedReceiptsHash) {
			//TODO 厳密化が必要。
			logger.warn("<Blockchain> RECEIPT HASH UNMATCH [%d]: given: %s != calc: %s. Block is %s".format(block.blockNumber, block.receiptsRoot, calculatedReceiptsHash, block.encode))
			//return
		}
		val calculatedLogBloomHash = LogBloomFilterCalculator.calculateLogBloomFilter(receipts)
		if (block.logsBloom != calculatedLogBloomHash) {
			//TODO 厳密化が必要。
			logger.warn("<Blockchain> LOG BLOOM FILTER UNMATCH [%d]: given: %s != calc: %s. Block is %s".format(block.blockNumber, block.logsBloom, calculatedLogBloomHash, block.encode))
			//return
		}
		track.commit()

		//ブロックを保存する。
		storeBlock(block, receipts)
		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> %s is stored. Total difficulty is %,d".format(block.summaryString(short = true), this.totalDifficulty))
		}

		if (needsFlushing(block)) {
			val startNanos = System.nanoTime
			logger.info("<Blockchain> Flushing data.")
			this.repository.flush()
			val reposEndNanos = System.nanoTime
			logger.info("<Blockchain> Flushed repos in %,d nanos.".format(reposEndNanos - startNanos))

			this.blockStore.flush()
			val endNanos = System.nanoTime
			logger.info("<Blockchain> Flushed block store in %,d nanos.".format(endNanos - reposEndNanos))

			System.gc()
			val gcEndNanos = System.nanoTime
			logger.info("<Blockchain> GC executed in %,d nanos.".format(gcEndNanos - endNanos))
		}
		//approve されたのでペンディング状態を解消する。
		this.wallet.removeTransactions(block.transactions)
		clearPendingTransactions(block.transactions)
		clearOutdatedTransactions(block.blockNumber)

		this.listener.trace("Block chain size: [%,d]".format(this.size))
		this.listener.onBlock(block, receipts)

		if (logger.isDebugEnabled) {
			logger.debug("<Blockchain> The block is successfully appended: %s. Chain size is now %,d.".format(block.summaryString(short = true), this.size))
		}
	}

	private def processBlock(block: Block): Seq[TransactionReceipt] = {
		if (!block.isGenesis && !SystemProperties.CONFIG.blockchainOnly) {
			this.wallet.addTransactions(block.transactions)
			val result = applyBlock(block)
			this.wallet.processBlock(block)

			logger.info("<Blockchain> Block[%,d] processed. Summary: [%s]. TxSize=%,d; Chain=[TD=%,d; StateRoot=%s]".format(
				block.blockNumber, block.summaryString(short = true), result.size, this.totalDifficulty, this.repository.rootHash)
			)
			for (i <- result.indices) {
				val receipt = result(i)
				val tx = receipt.transaction
				logger.info("<Blockchain> Block[%,d] processed. Tx[%,d]=%s; ManaUsed=%,d; AccumManaUsed=%,d".format(
					block.blockNumber, i, tx.summaryString, receipt.manaUsedForTx, receipt.cumulativeMana.toPositiveBigInt
				))
			}
			result
		} else {
			if (logger.isDebugEnabled) {
				logger.info("<Blockchain> Skipping block processing: %s (Genesis? %s, BlockchainOnly? %s).".format(
					block.summaryString(short = true), block.isGenesis, SystemProperties.CONFIG.blockchainOnly)
				)
			}
			Seq.empty
		}
	}

	/**
	 * ブロックに含まれるトランザクションを自ノードで実行し、状態を反映させます。
	 */
	private def applyBlock(block: Block): IndexedSeq[TransactionReceipt] = {
		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> Applying block: %s, TxSize=%,d".format(block.summaryString(short = true), block.transactions.size))
		}

		val startTime = System.nanoTime

		val receipts = new ArrayBuffer[TransactionReceipt]
		var cumulativeManaUsed = 0L
		for (tx <- block.transactions) {
			if (logger.isDebugEnabled) {
				logger.debug("<Blockchain> Executing %s".format(tx.summaryString))
			}

			val executor = new TransactionExecutor(tx, block.coinbase, this.track, this.blockStore, this._programInvokeFactory, block, this.listener, cumulativeManaUsed)
			executor.init()
			executor.execute()
			executor.go()
			executor.finalization()

			//CPUの貧弱な環境において、トランザクションの実行にばかり夢中になっていると
			//ping通信などを怠ってしまう可能性があるため、
			//トランザクションとトランザクションの間で、明示的に他のスレッドに譲る。
			//トランザクションの中身が重い場合には、さらにVM内でも同様の謙譲がある。
			Thread.`yield`()

			val manaUsedForTx = executor.manaUsed
			cumulativeManaUsed += manaUsedForTx

			if (logger.isTraceEnabled) {
				logger.trace("<Blockchain> ManaUsed=%,d for %s".format(manaUsedForTx, tx.summaryString))
			}

			this.track.commit()
			val accumUsedManaBytes = ImmutableBytes.asUnsignedByteArray(BigInt(cumulativeManaUsed))
			val receipt = TransactionReceipt(this.repository.rootHash, accumUsedManaBytes, Bloom(), executor.logs)
			receipt.transaction = tx
			receipt.manaUsedForTx = manaUsedForTx

			receipts.append(receipt)
		}
		addReward(block)
		updateTotalDifficulty(block)

		this.track.commit()
		val endTime = System.nanoTime

		//this.adminInfo.addBlockExecNanos(endTime - startTime)
		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> Applied block: %s, TxSize=%,d. ElapsedNanos=%,d.".format(block.summaryString(short = true), block.transactions.size, endTime - startTime))
		}
		receipts.toIndexedSeq
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
		if (logger.isDebugEnabled) {
			logger.debug("<Blockchain> Total reward for %s is %,d. (Uncles=%,d)".format(block.summaryString(short = true), totalBlockReward, block.uncles.size))
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

	def getParentOf(header: BlockHeader): Option[Block] = this.blockStore.getBlockByHash(header.parentHash)

	private def isValid(block: Block): Boolean = {
		if (logger.isTraceEnabled) {
			logger.trace("<Blockchain> Validating %s".format(block.summaryString(short = true)))
		}

		if (block.isGenesis) {
			if (logger.isDebugEnabled) {
				logger.debug("<Blockchain> [Valid] Genesis block.")
			}
			return true
		}
		if (!isValid(block.blockHeader)) {
			logger.info("<Blockchain> [Invalid] BAD BLOCK HEADER.")
			return false
		}
		if (block.txTrieRoot != TxTrieRootCalculator.calculateTxTrieRoot(block.transactions)) {
			logger.info("<Blockchain> [Invalid] BAD TX HASH: %s != %s".format(block.txTrieRoot, TxTrieRootCalculator.calculateTxTrieRoot(block.transactions)))
			return false
		}
		//Uncleに関する規則を遵守しているか。
		val unclesRule = new UnclesRule
		if (!unclesRule.validate(block)) {
			unclesRule.errors.foreach {
				each => logger.info("<Blockchain> [Invalid] %s".format(each))
			}
			return false
		}
		//Uncleのヘッダ自身および世代数の検査をする。
		for (uncle <- block.uncles) {
			if (!isValid(uncle)) {
				logger.info("<Blockchain> [Invalid] BAD UNCLE HEADER.")
				return false
			}
			if (getParentOf(uncle).get.blockNumber < (block.blockNumber - UnclesRule.UncleGenerationLimit)) {
				if (logger.isDebugEnabled) {
					val commonAncestor = getParentOf(uncle).get.blockNumber
					val diff = block.blockNumber - commonAncestor
					logger.info("<Blockchain> [Invalid] UNCLE TOO OLD: %d (limit) < %d (actual)".format(UnclesRule.UncleGenerationLimit, diff))
				}
				return false
			}
		}
		if (logger.isDebugEnabled) {
			logger.debug("<Blockchain> [Valid] %s".format(block.summaryString(short = true)))
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
				logger.info("<Blockchain> [Invalid] FAILED TO FIND PARENT (%s) of %,d".format(header.parentHash, header.blockNumber))
				false
		}
	}

	override def hasParentOnTheChain(block: Block) = getParentOf(block.blockHeader).isDefined

	override def close() = ()

	override def updateTotalDifficulty(block: Block) = {
		this._totalDifficulty += block.difficultyAsBigInt
		if (logger.isDebugEnabled) {
			logger.debug("<Blockchain> Total difficulty is updated to %,d".format(this._totalDifficulty))
		}
	}

	override def storeBlock(block: Block, receipts: Seq[TransactionReceipt]): Unit = {
		if (!SystemProperties.CONFIG.blockchainOnly) {
			if (block.stateRoot != this.repository.rootHash) {
				val message = "<Blockchain> State conflict at %s: %s != %s".format(block.summaryString(short = true), block.stateRoot, this.repository.rootHash)
				println(message)
				stateLogger.warn(message)
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
			this.repository.getAccountState(tx.senderAddress) match {
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
			if (logger.isDebugEnabled) {
				logger.debug("<BlockChainImpl> Clear tx: hash=%s".format(tx.hash.toShortString))
			}
			this._pendingTransactions.remove(PendingTransaction(tx, 0))
		}
	}

	private def clearOutdatedTransactions(blockNumber: Long): Unit = {
		val outdated = new ArrayBuffer[PendingTransaction]
		val transactions = new ArrayBuffer[TransactionLike]
		this._pendingTransactions.synchronized {
			for (tx <- this._pendingTransactions) {
				if (SystemProperties.CONFIG.txOutdatedThreshold < blockNumber - tx.blockNumer) {
					if (logger.isDebugEnabled) {
						logger.debug("<BlockChainImpl> Deleting outdated tx: hash=%s".format(tx.hash.toShortString))
					}
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