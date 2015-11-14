package org.lipicalabs.lipica.core.db

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.base.{Block, AccountState, Repository}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.datasource.{HashMapDB, KeyValueDataSource}
import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable

/**
 * Repository の実装基底クラスです。
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class RepositoryImpl(private var detailsDS: KeyValueDataSource, private var stateDS: KeyValueDataSource) extends Repository {

	def this() = this(new HashMapDB, new HashMapDB)

	import RepositoryImpl._


	detailsDS.setName(DETAILS_DB)
	detailsDS.init()


	stateDS.setName(STATE_DB)
	stateDS.init()

	private var dds = new DetailsDataStore
	private var detailsDB = new DatabaseImpl(detailsDS)
	dds.db = detailsDB

	private var stateDB = new DatabaseImpl(stateDS)
	private var worldState = new SecureTrie(stateDB.getDB)

	private var _isClosed: Boolean = false

	private val lock: ReentrantLock = new ReentrantLock
	private val accessCounter = new AtomicInteger(0)

	private var isSnapshot = false


	private def withLock[T](f: () => T): T = {
		this.lock.lock()
		try {
			while (0 < this.accessCounter.get) {
				Thread.sleep(100L)
			}
			f.apply()
		} finally {
			this.lock.unlock()
		}
	}

	private def withAccessCounting[T](f: () => T): T = {
		while (this.lock.isLocked) {
			Thread.sleep(100L)
		}
		this.accessCounter.incrementAndGet
		try {
			f.apply()
		} finally {
			this.accessCounter.decrementAndGet
		}
	}

	override def reset(): Unit = {
		withLock {
			() => {
				close()
				this.detailsDS.init()
				this.detailsDB = new DatabaseImpl(this.detailsDS)

				this.stateDS.init()
				this.stateDB = new DatabaseImpl(this.stateDS)
				this.worldState = new SecureTrie(this.stateDB.getDB)
			}
		}
	}

	override def close(): Unit = {
		withLock {
			() => {
				if (!isClosed) {
					this.detailsDB.close()
					this.stateDB.close()
					this._isClosed = true
				}
			}
		}
	}

	override def isClosed: Boolean = {
		this._isClosed
	}

	override def updateBatch(stateCache: mutable.Map[ImmutableBytes, AccountState], detailsCache: mutable.Map[ImmutableBytes, ContractDetails]): Unit = {
		logger.info("<RepositoryImpl> UpdatingBatch: detailsCache.size: %,d".format(detailsCache.size))
		for (eachEntry <- stateCache) {
			val (hash, accountState) = eachEntry
			var contractDetails = detailsCache.get(hash).get

			if (accountState.isDeleted) {
				delete(hash)
				logger.debug("<RepositoryImpl> Delete: [%s]".format(hash.toHexString))
			} else if (contractDetails.isDirty) {
				val contractDetailsCache = contractDetails.asInstanceOf[ContractDetailsCacheImpl]
				if (contractDetailsCache.originalContract eq null) {
					contractDetailsCache.originalContract = new ContractDetailsImpl
					contractDetailsCache.originalContract.setAddress(hash)
					contractDetailsCache.commit()
				}
				contractDetails = contractDetailsCache.originalContract
				updateContractDetails(hash, contractDetails)
				if (accountState.codeHash != DigestUtils.EmptyTrieHash) {
					accountState.stateRoot = contractDetails.getStorageHash
				}
				updateAccountState(hash, accountState)
				if (logger.isDebugEnabled) {
					logger.debug("<RepositoryImpl> Update: [%s], nonce: [%s], balance: [%s] \n [%s]".format(
						hash.toHexString, accountState.nonce, accountState.balance, contractDetails.getStorage
					))
				}
			}
		}
		logger.info("<RepositoryImpl> UpdatingBatch: detailsCache.size: %,d".format(detailsCache.size))
		stateCache.clear()
		detailsCache.clear()
	}

	override def flushNoReconnect(): Unit = {
		withLock {
			() => {
				gLogger.info("<RepositoryImpl> Flushing to disk.")
				this.dds.flush()
				this.worldState.sync()
			}
		}
	}

	override def flush(): Unit = {
		withLock {
			() => {
				gLogger.info("<RepositoryImpl> Flushing to disk.")
				this.dds.flush()
				this.worldState.sync()

				val rootHash = this.worldState.rootHash
				reset()
				this.worldState.root = rootHash
			}
		}
	}

	override def rollback(): Unit = throw new UnsupportedOperationException

	override def commit(): Unit = throw new UnsupportedOperationException

	override def syncToRoot(v: ImmutableBytes): Unit = {
		withAccessCounting {
			() => {
				this.worldState.root = v
			}
		}
	}

	override def startTracking: Repository = new RepositoryTrack(this)

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes): Unit = {
		//TODO 未実装。
	}

	override def getAccountKeys: Set[ImmutableBytes] = {
		withAccessCounting {
			() => {
				this.dds.keys.filter(existsAccount)
			}
		}
	}

	private def loadAccountStateOrCreateNew (address : ImmutableBytes): AccountState = {
		getAccountState(address).getOrElse(createAccount(address))
	}

	private def updateAccountState(address: ImmutableBytes, account: AccountState): Unit = {
		withAccessCounting {
			() => {
				this.worldState.update(address, account.encode)
			}
		}
	}

	override def addBalance(address: ImmutableBytes, value: BigInt): BigInt = {
		val account = loadAccountStateOrCreateNew(address)
		val result = account.addToBalance(value)
		updateAccountState(address, account)
		result
	}

	override def getBalance(address: ImmutableBytes): Option[BigInt] = {
		getAccountState(address).map(_.balance)
	}

	override def getStorageValue(address: ImmutableBytes, key: DataWord): Option[DataWord] = {
		getContractDetails(address).map(_.get(key))
	}

	override def getStorage(address: ImmutableBytes, keys: Iterable[DataWord]): Map[DataWord, DataWord] = {
		getContractDetails(address).map(_.getStorage(keys)).getOrElse(Map.empty)
	}

	private def updateContractDetails(address: ImmutableBytes, details: ContractDetails): Unit = {
		withAccessCounting {
			() => {
				this.dds.update(address, details)
			}
		}
	}

	override def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord): Unit = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}
		details.put(key, value)
		updateContractDetails(address, details)
	}

	override def getCode(address: ImmutableBytes): Option[ImmutableBytes] = {
		getAccountState(address) match {
			case Some(account) =>
				val codeHash = account.codeHash
				if (codeHash == DigestUtils.EmptyDataHash) {
					return Some(ImmutableBytes.empty)
				}
				getContractDetails(address).map(_.getCode)
			case _ =>
				None
		}
	}

	override def saveCode(address: ImmutableBytes, code: ImmutableBytes): Unit = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}
		details.setCode(code)
		val account = getAccountState(address).get
		account.codeHash = code.sha3

		updateContractDetails(address, details)
		updateAccountState(address, account)
	}

	override def getNonce(address: ImmutableBytes): BigInt = loadAccountStateOrCreateNew(address).nonce

	override def increaseNonce(address: ImmutableBytes): BigInt = {
		val account = loadAccountStateOrCreateNew(address)
		account.incrementNonce()
		updateAccountState(address, account)
		account.nonce
	}

	def setNonce(address: ImmutableBytes, nonce: BigInt): BigInt = {
		val account = loadAccountStateOrCreateNew(address)
		account.nonce = nonce
		updateAccountState(address, account)
		account.nonce
	}

	override def delete(address: ImmutableBytes): Unit = {
		withAccessCounting {
			() => {
				this.worldState.delete(address)
			}
		}
	}

	override def getContractDetails(address: ImmutableBytes): Option[ContractDetails] = {
		withAccessCounting {
			() => {
				val storageRoot = getAccountState(address).map(_.stateRoot).getOrElse(DigestUtils.EmptyTrieHash)
				this.dds.get(address).map(_.getSnapshotTo(storageRoot))
			}
		}
	}

	override def getAccountState(address: ImmutableBytes): Option[AccountState] = {
		withAccessCounting {
			() => {
				val bytes = this.worldState.get(address)
				if (bytes.nonEmpty) {
					Some(new AccountState(bytes))
				} else {
					None
				}
			}
		}
	}

	override def createAccount(address: ImmutableBytes): AccountState = {
		val account = new AccountState()
		updateAccountState(address, account)
		updateContractDetails(address, new ContractDetailsImpl())
		account
	}

	override def existsAccount(address: ImmutableBytes): Boolean = getAccountState(address).nonEmpty



	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit = {
		val account = getAccountState(address).map(_.createClone).getOrElse(new AccountState())
		cacheAccounts.put(address, account)

		val details = new ContractDetailsCacheImpl(getContractDetails(address).orNull)
		cacheDetails.put(address, details)
	}

	override def getSnapshotTo(root: ImmutableBytes): Repository = {
		val trie = new SecureTrie(this.stateDS)
		trie.root = root
		trie.cache = this.worldState.cache

		val repo = new RepositoryImpl()
		repo.worldState = trie
		repo.stateDB = this.stateDB
		repo.stateDS = this.stateDS

		repo.detailsDB = this.detailsDB
		repo.detailsDS = this.detailsDS

		repo.dds = this.dds
		repo.isSnapshot = true

		repo
	}

	override def getRoot: ImmutableBytes = this.worldState.rootHash

}

object RepositoryImpl {
	private val DETAILS_DB: String = "details"
	private val STATE_DB: String = "state"
	private val logger: Logger = LoggerFactory.getLogger("repository")
	private val gLogger: Logger = LoggerFactory.getLogger("general")
}
