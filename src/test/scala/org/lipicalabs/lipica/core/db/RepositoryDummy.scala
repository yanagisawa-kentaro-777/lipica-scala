package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.base.{Block, AccountState, Repository}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class RepositoryDummy extends Repository {
	override def createAccount(address: ImmutableBytes): AccountState = ???

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	override def getAccountKeys: Set[ImmutableBytes] = ???

	/**
	 * アカウントを取得します。
	 */
	override def getAccountState(address: ImmutableBytes): Option[AccountState] = ???

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes): Unit = ???

	/**
	 * 指定されたアカウントの残高に、指定された値を足します。
	 */
	override def addBalance(address: ImmutableBytes, value: BigInt): BigInt = ???

	/**
	 * 指定されたアカウントの残高を返します。
	 */
	override def getBalance(address: ImmutableBytes): Option[BigInt] = ???

	/**
	 * アカウントの存否確認を行います。
	 * @param address 検査対象のアカウント。
	 */
	override def existsAccount(address: ImmutableBytes): Boolean = ???

	/**
	 * 指定されたアカウントに対して、キーと値の組み合わせを登録します。
	 */
	override def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord): Unit = ???

	override def startTracking: Repository = ???

	/**
	 * 指定されたアカウントに結び付けられたコードを読み取ります。
	 */
	override def getCode(address: ImmutableBytes): Option[ImmutableBytes] = ???

	override def rollback(): Unit = ???

	override def flushNoReconnect(): Unit = ???

	override def flush(): Unit = ???

	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit = ???

	override def getSnapshotTo(root: ImmutableBytes): Repository = ???

	override def updateBatch(accountStates: mutable.Map[ImmutableBytes, AccountState], contractDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit = ???

	/**
	 * アカウントを削除します。
	 */
	override def delete(address: ImmutableBytes): Unit = ???

	override def getRoot: ImmutableBytes = ???

	/**
	 * 指定されたアカウントに対応するコントラクト明細を取得して返します。
	 */
	override def getContractDetails(address: ImmutableBytes): Option[ContractDetails] = None

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	override def getStorageValue(address: ImmutableBytes, key: DataWord): Option[DataWord] = ???

	override def getStorage(address: ImmutableBytes, keys: Iterable[DataWord]) = ???

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 */
	override def getNonce(address: ImmutableBytes): BigInt = ???

	override def close(): Unit = ???

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 */
	override def increaseNonce(address: ImmutableBytes): BigInt = ???

	override def isClosed: Boolean = ???

	override def reset(): Unit = ???

	override def syncToRoot(root: ImmutableBytes): Unit = ???

	/**
	 * 指定されたアカウントに対して、コードを保存します。
	 */
	override def saveCode(address: ImmutableBytes, code: ImmutableBytes): Unit = ???

	override def commit(): Unit = ???
}
