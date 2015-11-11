package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.{Block, AccountState, Repository}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/09 20:37
 * YANAGISAWA, Kentaro
 */
class RepositoryTrack(private val repository: Repository) extends Repository {

	import RepositoryTrack._

	private val cacheAccounts = new mutable.HashMap[ImmutableBytes, AccountState]
	private val cacheDetails = new mutable.HashMap[ImmutableBytes, ContractDetails]

	override def createAccount(address: ImmutableBytes) = {
		if (logger.isTraceEnabled) {
			logger.trace("<RepositoryTrack> Creating account: [%s]".format(address))
		}
		val accountState = new AccountState
		this.cacheAccounts.put(address, accountState)

		val contractDetails = new ContractDetailsImpl
		contractDetails.isDirty = true
		this.cacheDetails.put(address, contractDetails)

		accountState
	}

	override def getAccountState(address: ImmutableBytes) = {
		this.cacheAccounts.get(address) match {
			case Some(account) => Some(account)
			case _ =>
				this.repository.loadAccount(address, this.cacheAccounts, this.cacheDetails)
				this.cacheAccounts.get(address)
		}
	}

	override def existsAccount(address: ImmutableBytes) = {
		this.cacheAccounts.get(address) match {
			case Some(account) => !account.isDeleted
			case _ => this.repository.existsAccount(address)
		}
	}

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	override def getAccountKeys = ???

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = ???

	/**
	 * 指定されたアカウントの残高に、指定された値を足します。
	 */
	override def addBalance(address: ImmutableBytes, value: BigInt) = ???

	/**
	 * 指定されたアカウントの残高を返します。
	 */
	override def getBalance(address: ImmutableBytes) = ???


	/**
	 * 指定されたアカウントに対して、キーと値の組み合わせを登録します。
	 */
	override def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord) = ???

	override def startTracking = ???

	/**
	 * 指定されたアカウントに結び付けられたコードを読み取ります。
	 */
	override def getCode(address: ImmutableBytes) = ???

	override def rollback() = ???

	override def flushNoReconnect() = ???

	override def flush() = ???

	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]) = ???

	override def getSnapshotTo(root: ImmutableBytes) = ???

	override def updateBatch(accountStates: Map[ImmutableBytes, AccountState], contractDetails: Map[ImmutableBytes, ContractDetails]) = ???

	/**
	 * アカウントを削除します。
	 */
	override def delete(address: ImmutableBytes) = ???

	override def getRoot = ???

	/**
	 * 指定されたアカウントに対応するコントラクト明細を取得して返します。
	 */
	override def getContractDetails(address: ImmutableBytes) = ???

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	override def getStorageValue(address: ImmutableBytes, key: DataWord) = ???

	override def getStorage(address: ImmutableBytes, keys: Iterable[DataWord]): Map[DataWord, DataWord] = ???

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 */
	override def getNonce(address: ImmutableBytes) = ???

	override def close() = ???

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 */
	override def increaseNonce(address: ImmutableBytes) = ???

	override def isClosed = ???

	override def reset() = ???

	override def syncToRoot(root: ImmutableBytes) = ???

	/**
	 * 指定されたアカウントに対して、コードを保存します。
	 */
	override def saveCode(address: ImmutableBytes, code: ImmutableBytes) = ???

	override def commit() = ???
}

object RepositoryTrack {
	private val logger = LoggerFactory.getLogger("repository")
}