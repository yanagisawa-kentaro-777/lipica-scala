package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.db.ContractDetails
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * データを保存するリポジトリが実装すべき trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 13:46
 * YANAGISAWA, Kentaro
 */
trait Repository {

	def createAccount(address: ImmutableBytes): AccountState

	/**
	 * アカウントの存否確認を行います。
	 * @param address 検査対象のアカウント。
	 */
	def existsAccount(address: ImmutableBytes): Boolean

	/**
	 * アカウントを取得します。
	 */
	def getAccountState(address: ImmutableBytes): AccountState

	/**
	 * アカウントを削除します。
	 */
	def delete(address: ImmutableBytes): Unit

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 */
	def increaseNonce(address: ImmutableBytes): BigInt

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 */
	def getNonce(address: ImmutableBytes): BigInt

	/**
	 * 指定されたアカウントに対応するコントラクト明細を取得して返します。
	 */
	def getContractDetails(address: ImmutableBytes): ContractDetails

	/**
	 * 指定されたアカウントに対して、コードを保存します。
	 */
	def saveCode(address: ImmutableBytes, code: ImmutableBytes): Unit

	/**
	 * 指定されたアカウントに結び付けられたコードを読み取ります。
	 */
	def getCode(address: ImmutableBytes): ImmutableBytes

	/**
	 * 指定されたアカウントに対して、キーと値の組み合わせを登録します。
	 */
	def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord): Unit

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	def getStorageValue(address: ImmutableBytes, key: DataWord): Option[DataWord]

	/**
	 * 指定されたアカウントの残高を返します。
	 */
	def getBalance(address: ImmutableBytes): BigInt

	/**
	 * 指定されたアカウントの残高に、指定された値を足します。
	 */
	def addBalance(address: ImmutableBytes, value: BigInt): BigInt

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	def getAccountKeys: Set[ImmutableBytes]

	def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: Array[Byte])

	def startTracking: Repository

	def flush(): Unit

	def flushNoReconnect(): Unit

	def commit(): Unit

	def rollback(): Unit

	def syncToRoot(root: Array[Byte])

	def close(): Unit

	def isClosed: Boolean

	def reset(): Unit

	def getRoot: Array[Byte]

	def updateBatch(accountStates: Map[ImmutableBytes, AccountState], contractDetails: Map[ImmutableBytes, ContractDetails])

	def loadAccount(address: ImmutableBytes, cacheAccounts: Map[ImmutableBytes, AccountState], cacheDetails: Map[ImmutableBytes, ContractDetails])

	def getSnapshotTo(root: Array[Byte]): Repository

}

object Repository {

	def transfer(repository: Repository, fromAddress: ImmutableBytes, toAddress: ImmutableBytes, value: BigInt): Unit = {
		repository.addBalance(fromAddress, -value)
		repository.addBalance(fromAddress, value)
	}

}