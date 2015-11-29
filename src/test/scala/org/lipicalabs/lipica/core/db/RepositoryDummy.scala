package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.{ContractDetailsImpl, ContractDetails, Block, AccountState}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable


/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class RepositoryDummy extends RepositoryImpl {

	private val worldState = new mutable.HashMap[ImmutableBytes, AccountState]
	private val detailsDB = new mutable.HashMap[ImmutableBytes, ContractDetails]

	override def reset() = {
		this.worldState.clear()
		this.detailsDB.clear()
	}

	override def close() = {
		this.worldState.clear()
		this.detailsDB.clear()
	}

	override def isClosed = throw new UnsupportedOperationException

	override def flush() = throw new UnsupportedOperationException

	override def syncToRoot(root: ImmutableBytes) = throw new UnsupportedOperationException

	override def startTracking = new RepositoryTrack(this)

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = ()

	override def getAccountKeys = this.worldState.keySet.toSet

	override def addBalance(address: ImmutableBytes, value: BigInt) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		val result = account.addToBalance(value)
		worldState.put(address, account)
		result
	}

	override def getBalance(address: ImmutableBytes) = {
		getAccountState(address).map(_.balance)
	}

	override def getStorageValue(address: ImmutableBytes, key: DataWord) = {
		getContractDetails(address).flatMap(_.get(key))
	}

	override def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord) = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}

		details.put(key, value)
		detailsDB.put(address, details)
	}

	override def getCode(address: ImmutableBytes) = {
		getContractDetails(address).map(_.code)
	}

	override def saveCode(address: ImmutableBytes, code: ImmutableBytes) = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}
		details.code = code
	}

	override def getNonce(address: ImmutableBytes) = {
		getAccountState(address).getOrElse(createAccount(address)).nonce
	}

	override def increaseNonce(address: ImmutableBytes) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		account.incrementNonce()
		this.worldState.put(address, account)
		account.nonce
	}

	override def setNonce(address: ImmutableBytes, value: BigInt) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		account.nonce = value
		this.worldState.put(address, account)
		account.nonce
	}

	override def delete(address: ImmutableBytes) = {
		this.worldState.remove(address)
		this.detailsDB.remove(address)
	}

	override def getContractDetails(address: ImmutableBytes) = this.detailsDB.get(address)

	override def getAccountState(address: ImmutableBytes) = this.worldState.get(address)


	override def createAccount(address: ImmutableBytes) = {
		val account = new AccountState()
		this.worldState.put(address, account)
		this.detailsDB.put(address, new ContractDetailsImpl())
		account
	}

	override def existsAccount(address: ImmutableBytes) = getAccountState(address).isDefined


	override def rootHash = throw new UnsupportedOperationException

	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]) = {
		val account: AccountState = getAccountState(address).map(_.createClone).getOrElse(new AccountState())
		cacheAccounts.put(address, account)
		val details: ContractDetails = getContractDetails(address).map(_.createClone).getOrElse(new ContractDetailsImpl())
		cacheDetails.put(address, details)
	}

	override def updateBatch(accountCache: mutable.Map[ImmutableBytes, AccountState], detailsCache: mutable.Map[ImmutableBytes, ContractDetails]) = {
		for (entry <- accountCache) {
			val (hash, accountState) = entry
			val contractDetails = detailsCache.get(hash).get
			if (accountState.isDeleted) {
				worldState.remove(hash)
				detailsDB.remove(hash)
			} else {
				if (accountState.isDirty || contractDetails.isDirty) {
					detailsDB.put(hash, contractDetails)
					accountState.storageRoot = contractDetails.storageRoot
					accountState.codeHash = contractDetails.code.digest256
					worldState.put(hash, accountState)
				}
			}
		}
		accountCache.clear()
		detailsCache.clear()
	}

}
