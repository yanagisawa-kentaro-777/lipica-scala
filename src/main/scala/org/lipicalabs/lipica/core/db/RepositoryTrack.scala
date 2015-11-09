package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.{Block, AccountState, Repository}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/09 20:37
 * YANAGISAWA, Kentaro
 */
class RepositoryTrack(private val repository: Repository) extends Repository {
	override def createAccount(address: ImmutableBytes) = ???

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	override def getAccountKeys = ???

	/**
	 * アカウントを取得します。
	 */
	override def getAccountState(address: ImmutableBytes) = ???

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
	 * アカウントの存否確認を行います。
	 * @param address 検査対象のアカウント。
	 */
	override def existsAccount(address: ImmutableBytes) = ???

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

	override def updateBatch(accountStates: mutable.Map[ImmutableBytes, AccountState], contractDetails: mutable.Map[ImmutableBytes, ContractDetails]) = ???

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
