package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.kernel._
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.DataWord
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * 頻繁な更新操作を一時的にキャッシュするための Repository 実装です。
 * Repository#startTracking によってインスタンスが生成されます。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/09 20:37
 * YANAGISAWA, Kentaro
 */
class RepositoryTrack private[datastore](private val repository: RepositoryLike) extends RepositoryTrackLike {

	import RepositoryTrack._

	private val cacheAccounts = new mutable.HashMap[Address, AccountState]
	private val cacheDetails = new mutable.HashMap[Address, ContractDetails]

	override def createAccount(address: Address) = {
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Creating account: [%s]".format(address))
		}
		val accountState = new AccountState
		this.cacheAccounts.put(address, accountState)

		val contractDetails = new ContractDetailsCacheImpl(null)
		contractDetails.isDirty = true
		this.cacheDetails.put(address, contractDetails)

		accountState
	}

	override def getAccountState(address: Address) = {
		this.cacheAccounts.get(address) match {
			case Some(account) =>
				if (logger.isTraceEnabled) {
					logger.trace("<RepositoryTrack> Exists cached account state: %s -> %,d".format(address, account.balance))
				}
				Some(account)
			case _ =>
				this.repository.loadAccount(address, this.cacheAccounts, this.cacheDetails)
				val result = this.cacheAccounts.get(address)
				if (logger.isTraceEnabled && result.isDefined) {
					logger.trace("<RepositoryTrack> Loaded account state: %s -> %,d".format(address, result.get.balance))
				}
				result
		}
	}

	override def existsAccount(address: Address) = {
		this.cacheAccounts.get(address) match {
			case Some(account) => !account.isDeleted
			case _ => this.repository.existsAccount(address)
		}
	}

	override def getContractDetails(address: Address) = {
		this.cacheDetails.get(address) match {
			case Some(details) => Some(details)
			case _ =>
				this.repository.loadAccount(address, this.cacheAccounts, this.cacheDetails)
				this.cacheDetails.get(address)
		}
	}

	override def loadAccount(address: Address, aCacheAccounts: mutable.Map[Address, AccountState], aCacheDetails: mutable.Map[Address, ContractDetails]) = {
		val (account, details) =
			this.cacheAccounts.get(address) match {
				case Some(accountState) =>
					val contractDetails = this.cacheDetails.get(address).get
					(accountState, contractDetails)
				case _ =>
					this.repository.loadAccount(address, this.cacheAccounts, this.cacheDetails)
					(this.cacheAccounts.get(address).get, this.cacheDetails.get(address).get)
			}
		aCacheAccounts.put(address, account.createClone)
		aCacheDetails.put(address, new ContractDetailsCacheImpl(details))
	}

	override def delete(address: Address) = {
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Delete account: [%s]".format(address.toHexString))
		}
		getAccountState(address).foreach(_.isDeleted = true)
		getContractDetails(address).foreach(_.isDeleted = true)
	}

	override def increaseNonce(address: Address) = {
		val accountState = getAccountState(address).getOrElse(createAccount(address))
		getContractDetails(address).foreach(_.isDirty = true)

		val prevNonce = accountState.nonce
		accountState.incrementNonce()
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Increased nonce: [%s] from [%s] to [%s]".format(address, prevNonce, accountState.nonce))
		}
		accountState.nonce
	}

	def setNonce(address: Address, v: BigInt): BigInt = {
		val accountState = getAccountState(address).getOrElse(createAccount(address))
		getContractDetails(address).foreach(_.isDirty = true)

		val prevNonce = accountState.nonce
		accountState.nonce = v
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Set nonce: [%s] from [%s] to [%s]".format(address, prevNonce, accountState.nonce))
		}
		accountState.nonce
	}

	override def getNonce(address: Address) = getAccountState(address).map(_.nonce).getOrElse(UtilConsts.Zero)

	override def getBalance(address: Address) = getAccountState(address).map(_.balance)

	override def addBalance(address: Address, value: BigInt) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryTrack> Adding to balance: [%s] Balance: [%,d], Delta: [%,d]".format(address, account.balance, value))
		}
		getContractDetails(address).foreach(_.isDirty = true)
		val newBalance = account.addToBalance(value)
		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryTrack> Added to balance: [%s] NewBalance: [%,d], Delta: [%,d]".format(address, newBalance, value))
		}
		newBalance
	}

	override def saveCode(address: Address, code: ImmutableBytes) = {
		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryTrack> Saving code. Address: [%s], Code: [%s]".format(address.toHexString, code.toHexString))
		}
		getContractDetails(address).foreach {
			each => {
				each.code = code
				each.isDirty = true
			}
		}
		getAccountState(address).foreach(_.codeHash = code.digest256)
	}

	override def getCode(address: Address): Option[ImmutableBytes] = {
		if (!existsAccount(address)) {
			None
		} else if (getAccountState(address).get.codeHash == DigestUtils.EmptyDataHash) {
			None
		} else {
			Option(getContractDetails(address).get.code)
		}
	}

	override def addStorageRow(address: Address, key: DataWord, value: DataWord) = {
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Add storage row. Address: [%s], Key: [%s], Value: [%s]".format(address.toHexString, key.toHexString, value.toHexString))
		}
		getContractDetails(address).foreach(_.put(key, value))
	}

	override def getStorageValue(address: Address, key: DataWord) = getContractDetails(address).flatMap(_.get(key))

	override def getAccountKeys = throw new UnsupportedOperationException

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = throw new UnsupportedOperationException

	override def startTracking = new RepositoryTrack(this)

	override def commit() = {
		if (this.cacheAccounts.nonEmpty || this.cacheDetails.nonEmpty) {
			for (details <- this.cacheDetails.values) {
				details.asInstanceOf[ContractDetailsCacheImpl].commit()
			}
			this.repository.updateBatch(this.cacheAccounts, this.cacheDetails)
			this.cacheAccounts.clear()
			this.cacheDetails.clear()
			if (logger.isDebugEnabled) {
				logger.debug("<RepositoryTrack> Committed changes.")
			}
		} else {
			logger.debug("<RepositoryTrack> No data to commit.")
		}
	}

	override def rollback() = {
		this.cacheAccounts.clear()
		this.cacheDetails.clear()

		if (logger.isDebugEnabled) {
			logger.debug("<RepositoryTrack> Rolled back changes.")
		}
	}

	override def updateBatch(accountStates: mutable.Map[Address, AccountState], contractDetails: mutable.Map[Address, ContractDetails]) = {
		for (each <- accountStates) {
			this.cacheAccounts.put(each._1, each._2)
		}
		for (each <- contractDetails) {
			val hash = each._1
			val contractDetailsCache = each._2.asInstanceOf[ContractDetailsCacheImpl]
			if (Option(contractDetailsCache.originalContract).exists(original => !original.isInstanceOf[ContractDetailsImpl])) {
				this.cacheDetails.put(hash, contractDetailsCache.originalContract)
			} else {
				this.cacheDetails.put(hash, contractDetailsCache)
			}
		}
	}

	override def getStorageContent(address: Address, keys: Iterable[DataWord]): Map[DataWord, DataWord] = {
		getContractDetails(address).map(_.storageContent(keys)).getOrElse(Map.empty)
	}

	override def originalRepository: RepositoryLike = {
		this.repository match {
			case r: RepositoryTrack => r.originalRepository
			case _ => this.repository
		}
	}

	override def close(): Unit = ()

}

object RepositoryTrack {
	private val logger = LoggerFactory.getLogger("repository")
}