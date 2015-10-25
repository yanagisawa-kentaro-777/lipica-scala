package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.db.ContractDetails
import org.lipicalabs.lipica.core.utils.ByteArrayWrapper
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * データを保存するリポジトリが実装すべき trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 13:46
 * YANAGISAWA, Kentaro
 */
trait Repository {

	def createAccount(addr: Array[Byte]): AccountState

	/**
	 * アカウントの存否確認を行います。
	 * @param addr 検査対象のアカウント。
	 */
	def existsAccount(addr: Array[Byte]): Boolean

	/**
	 * アカウントを取得します。
	 */
	def getAccountState(addr: Array[Byte]): AccountState

	/**
	 * アカウントを削除します。
	 */
	def delete(addr: Array[Byte]): Unit

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 */
	def increaseNonce(addr: Array[Byte]): BigInt

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 */
	def getNonce(addr: Array[Byte]): BigInt

	/**
	 * 指定されたアカウントに対応するコンタクト明細を取得して返します。
	 */
	def getContractDetails(addr: Array[Byte]): ContractDetails

	def saveCode(addr: Array[Byte], code: Array[Byte]): Unit

	def getCode(addr: Array[Byte]): Array[Byte]

	/**
	 * 指定されたアカウントに対して、キーと値の組み合わせを登録します。
	 */
	def addStorageRow(addr: Array[Byte], key: DataWord, value: DataWord): Unit

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	def getStorageValue(addr: Array[Byte], key: DataWord): DataWord

	/**
	 * 指定されたアカウントの残高を返します。
	 */
	def getBalance(addr: Array[Byte]): BigInt

	/**
	 * 指定されたアカウントの残高に、指定された値を足します。
	 */
	def addBalance(addr: Array[Byte], value: BigInt): BigInt

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	def getAccountKeys: Set[Array[Byte]]

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

	def updateBatch(accountStates: Map[ByteArrayWrapper, AccountState], contractDetails: Map[ByteArrayWrapper, ContractDetails])

	def loadAccount(addr: Array[Byte], cacheAccounts: Map[ByteArrayWrapper, AccountState], cacheDetails: Map[ByteArrayWrapper, ContractDetails])

	def getSnapshotTo(root: Array[Byte]): Repository

}
