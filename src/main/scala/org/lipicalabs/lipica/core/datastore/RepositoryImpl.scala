package org.lipicalabs.lipica.core.datastore

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference, AtomicInteger}
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.kernel._
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.datastore.datasource.{KeyValueDataSourceFactory, HashMapDB, KeyValueDataSource}
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
class RepositoryImpl(_detailsDS: KeyValueDataSource, _stateDS: KeyValueDataSource, private val dataSourceFactory: KeyValueDataSourceFactory) extends Repository {

	protected def this(dataSourceFactory: KeyValueDataSourceFactory) = this(new HashMapDB, new HashMapDB, dataSourceFactory)

	import RepositoryImpl._

	private val detailsDSRef = new AtomicReference[KeyValueDataSource](_detailsDS)
	def detailsDS: KeyValueDataSource = this.detailsDSRef.get

	private val detailsDBRef = new AtomicReference[DatabaseImpl](new DatabaseImpl(detailsDS))
	private def detailsDB: DatabaseImpl = this.detailsDBRef.get

	private val ddsRef = new AtomicReference[ContractDetailsStore](new ContractDetailsStore(this.detailsDB, this.dataSourceFactory))
	private def dds: ContractDetailsStore = this.ddsRef.get

	private val stateDSRef = new AtomicReference[KeyValueDataSource](_stateDS)
	private def stateDS: KeyValueDataSource = this.stateDSRef.get

	private val stateDBRef = new AtomicReference[DatabaseImpl](new DatabaseImpl(stateDS))
	private def stateDB: DatabaseImpl = this.stateDBRef.get

	private val worldStateRef = new AtomicReference[SecureTrie](new SecureTrie(stateDB.dataSource))
	private def worldState: SecureTrie = this.worldStateRef.get

	private val isClosedRef: AtomicBoolean = new AtomicBoolean(false)

	private val lock: ReentrantLock = new ReentrantLock
	private val accessCounter = new AtomicInteger(0)

	private val isSnapshotRef = new AtomicBoolean(false)
	def isSnapshot: Boolean = this.isSnapshotRef.get


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
				this.detailsDBRef.set(new DatabaseImpl(this.detailsDS))

				this.stateDS.init()
				this.stateDBRef.set(new DatabaseImpl(this.stateDS))

				this.worldStateRef.set(new SecureTrie(this.stateDB.dataSource))
			}
		}
	}

	override def close(): Unit = {
		withLock {
			() => {
				if (!isClosed) {
					this.detailsDB.close()
					this.stateDB.close()
					this.isClosedRef.set(true)
				}
			}
		}
	}

	override def isClosed: Boolean = this.isClosedRef.get

	override def updateBatch(stateCache: mutable.Map[ImmutableBytes, AccountState], detailsCache: mutable.Map[ImmutableBytes, ContractDetails]): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryImpl> Updating batch: accounts: %,d; contractDetails: %,d".format(stateCache.size, detailsCache.size))
		}
		for (eachEntry <- stateCache) {
			val (hash, accountState) = eachEntry
			var contractDetails = detailsCache.get(hash).get

			if (accountState.isDeleted) {
				delete(hash)
				logger.debug("<RepositoryImpl> Deleted: [%s]".format(hash.toHexString))
			} else if (contractDetails.isDirty) {
				logger.debug("<RepositoryImpl> Updating: [%s]".format(hash.toHexString))
				val contractDetailsCache = contractDetails.asInstanceOf[ContractDetailsCacheImpl]
				if (contractDetailsCache.originalContract eq null) {
					contractDetailsCache.originalContract = new ContractDetailsImpl(this.dataSourceFactory)
					contractDetailsCache.originalContract.address = hash
					contractDetailsCache.commit()
				}
				contractDetails = contractDetailsCache.originalContract
				updateContractDetails(hash, contractDetails)
				if (accountState.codeHash != DigestUtils.EmptyTrieHash) {
					accountState.storageRoot = contractDetails.storageRoot
				}
				updateAccountState(hash, accountState)
				if (logger.isDebugEnabled) {
					logger.debug("<RepositoryImpl> Update: Key=%s, Nonce=%,d; Balance=%,d, StorageSize=%,d".format(
						hash.toShortString, accountState.nonce, accountState.balance, contractDetails.storageSize
					))
				}
			} else {
				logger.debug("<RepositoryImpl> Passing: [%s]".format(hash.toShortString))
			}
		}
		stateCache.clear()
		detailsCache.clear()
		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryImpl> Updated batch: Accounts=%,d; ContractDetails=%,d".format(stateCache.size, detailsCache.size))
		}
	}

	override def flushNoReconnect(): Unit = {
		withLock {
			() => {
				logger.info("<RepositoryImpl> Flushing to disk.")
				this.dds.flush()
				this.worldState.sync()
			}
		}
	}

	override def flush(): Unit = {
		withLock {
			() => {
				logger.info("<Repos> Flushing to disk.")
				val startNanos = System.nanoTime
				this.dds.flush()
				val ddsEndNanos = System.nanoTime
				logger.info("<Repos> DDS flushed in %,d nanos.".format(ddsEndNanos - startNanos))

				this.worldState.sync()
				val stateEndNanos = System.nanoTime
				logger.info("<Repos> State flushed in %,d nanos.".format(stateEndNanos - ddsEndNanos))

				val rootHash = this.worldState.rootHash
				reset()
				this.worldState.root = rootHash
			}
		}
	}

	override def syncToRoot(v: ImmutableBytes): Unit = {
		withAccessCounting {
			() => {
				this.worldState.root = v
			}
		}
	}

	override def startTracking: RepositoryTrackLike = new RepositoryTrack(this)

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
				val encoded = account.encode
				this.worldState.update(address, encoded)
				if (logger.isTraceEnabled) {
					logger.trace("<RepositoryImpl> Updated account %s -> %s".format(address, encoded))
				}
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
		getContractDetails(address).flatMap(_.get(key))
	}

	override def getStorageContent(address: ImmutableBytes, keys: Iterable[DataWord]): Map[DataWord, DataWord] = {
		getContractDetails(address).map(_.storageContent(keys)).getOrElse(Map.empty)
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
					return None
				}
				getContractDetails(address).map(_.code)
			case _ =>
				None
		}
	}

	override def saveCode(address: ImmutableBytes, code: ImmutableBytes): Unit = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}
		details.code = code
		val account = getAccountState(address).get
		account.codeHash = code.digest256

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
				val storageRoot = getAccountState(address).map(_.storageRoot).getOrElse(DigestUtils.EmptyTrieHash)
				if (logger.isDebugEnabled) {
					logger.debug("<Repos> Loading contract details: %s".format(address.toHexString))
				}
				val contractDetailsOption = this.dds.get(address)
				if (logger.isDebugEnabled) {
					logger.debug("<Repos> Loaded contract details: %s".format(address.toHexString))
				}
				contractDetailsOption.map(_.getSnapshotTo(storageRoot))
			}
		}
	}

	override def getAccountState(address: ImmutableBytes): Option[AccountState] = {
		withAccessCounting {
			() => {
				val bytes = this.worldState.get(address)
				if (bytes.nonEmpty) {
					Some(AccountState.decode(bytes))
				} else {
					if (logger.isTraceEnabled) {
						logger.trace("<RepositoryImpl> Failed to load account state for %s".format(address))
					}
					None
				}
			}
		}
	}

	override def createAccount(address: ImmutableBytes): AccountState = {
		val account = new AccountState()
		updateAccountState(address, account)
		updateContractDetails(address, new ContractDetailsImpl(this.dataSourceFactory))
		account
	}

	override def existsAccount(address: ImmutableBytes): Boolean = getAccountState(address).nonEmpty



	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit = {
		val account = getAccountState(address).map(_.createClone).getOrElse(new AccountState())
		cacheAccounts.put(address, account)

		val details = new ContractDetailsCacheImpl(getContractDetails(address).orNull)
		cacheDetails.put(address, details)
	}

	override def createSnapshotTo(root: ImmutableBytes): Repository = {
		val trie = new SecureTrie(this.stateDS)
		trie.root = root
		trie.dataStore = this.worldState.dataStore

		val repo = new RepositoryImpl(this.dataSourceFactory)
		repo.worldStateRef.set(trie)

		repo.stateDBRef.set(this.stateDB)
		repo.stateDSRef.set(this.stateDS)

		repo.detailsDBRef.set(this.detailsDB)
		repo.detailsDSRef.set(this.detailsDS)

		repo.ddsRef.set(this.dds)
		repo.isSnapshotRef.set(true)

		repo
	}

	override def rootHash: ImmutableBytes = this.worldState.rootHash

}

object RepositoryImpl {
	private val logger: Logger = LoggerFactory.getLogger("database")
}
