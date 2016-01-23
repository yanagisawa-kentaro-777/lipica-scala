package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel._
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord

import scala.collection.mutable


/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class RepositoryDummy extends RepositoryImpl(new InMemoryDataSourceFactory) {

	private val worldState = new mutable.HashMap[Address, AccountState]
	private val detailsDB = new mutable.HashMap[Address, ContractDetails]

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

	override def syncToRoot(root: DigestValue) = throw new UnsupportedOperationException

	override def startTracking = new RepositoryTrack(this)

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = ()

	override def getAccountKeys = this.worldState.keySet.toSet

	override def addBalance(address: Address, value: BigInt) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		val result = account.addToBalance(value)
		worldState.put(address, account)
		result
	}

	override def getBalance(address: Address) = {
		getAccountState(address).map(_.balance)
	}

	override def getStorageValue(address: Address, key: VMWord) = {
		getContractDetails(address).flatMap(_.get(key))
	}

	override def addStorageRow(address: Address, key: VMWord, value: VMWord) = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}

		details.put(key, value)
		detailsDB.put(address, details)
	}

	override def getCode(address: Address) = {
		getContractDetails(address).map(_.code)
	}

	override def saveCode(address: Address, code: ImmutableBytes) = {
		val details = getContractDetails(address).getOrElse {
			createAccount(address)
			getContractDetails(address).get
		}
		details.code = code
	}

	override def getNonce(address: Address) = {
		getAccountState(address).getOrElse(createAccount(address)).nonce
	}

	override def increaseNonce(address: Address) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		account.incrementNonce()
		this.worldState.put(address, account)
		account.nonce
	}

	override def setNonce(address: Address, value: BigInt) = {
		val account = getAccountState(address).getOrElse(createAccount(address))
		account.nonce = value
		this.worldState.put(address, account)
		account.nonce
	}

	override def delete(address: Address) = {
		this.worldState.remove(address)
		this.detailsDB.remove(address)
	}

	override def getContractDetails(address: Address) = this.detailsDB.get(address)

	override def getAccountState(address: Address) = this.worldState.get(address)


	override def createAccount(address: Address) = {
		val account = new AccountState()
		this.worldState.put(address, account)
		this.detailsDB.put(address, new ContractDetailsImpl(new InMemoryDataSourceFactory))
		account
	}

	override def existsAccount(address: Address) = getAccountState(address).isDefined


	override def rootHash = throw new UnsupportedOperationException

	override def loadAccount(address: Address, cacheAccounts: mutable.Map[Address, AccountState], cacheDetails: mutable.Map[Address, ContractDetails]) = {
		val account: AccountState = getAccountState(address).map(_.createClone).getOrElse(new AccountState())
		cacheAccounts.put(address, account)
		val details: ContractDetails = getContractDetails(address).map(_.createClone).getOrElse(new ContractDetailsImpl(new InMemoryDataSourceFactory))
		cacheDetails.put(address, details)
	}

	override def updateBatch(accountCache: mutable.Map[Address, AccountState], detailsCache: mutable.Map[Address, ContractDetails]) = {
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
