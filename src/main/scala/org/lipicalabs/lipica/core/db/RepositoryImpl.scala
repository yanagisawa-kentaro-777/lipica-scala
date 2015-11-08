package org.lipicalabs.lipica.core.db

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import org.lipicalabs.lipica.core.base.{Block, AccountState, Repository}
import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.slf4j.{LoggerFactory, Logger}

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class RepositoryImpl(private val detailsDS: KeyValueDataSource, stateDS: KeyValueDataSource) extends Repository {

	import RepositoryImpl._

	private val lock: ReentrantLock = new ReentrantLock
	private val accessCounter = new AtomicInteger(0)

	override def createAccount(address: ImmutableBytes): AccountState = ???

	/**
	 * このアカウントのアドレスすべての集合を返します。
	 */
	override def getAccountKeys: Set[ImmutableBytes] = ???

	/**
	 * アカウントを取得します。
	 */
	override def getAccountState(address: ImmutableBytes): Option[AccountState] = ???

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: Array[Byte]): Unit = ???

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

	override def loadAccount(address: ImmutableBytes, cacheAccounts: Map[ImmutableBytes, AccountState], cacheDetails: Map[ImmutableBytes, ContractDetails]): Unit = ???

	override def getSnapshotTo(root: Array[Byte]): Repository = ???

	override def updateBatch(accountStates: Map[ImmutableBytes, AccountState], contractDetails: Map[ImmutableBytes, ContractDetails]): Unit = ???

	/**
	 * アカウントを削除します。
	 */
	override def delete(address: ImmutableBytes): Unit = ???

	override def getRoot: Array[Byte] = ???

	/**
	 * 指定されたアカウントに対応するコントラクト明細を取得して返します。
	 */
	override def getContractDetails(address: ImmutableBytes): Option[ContractDetails] = ???

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	override def getStorageValue(address: ImmutableBytes, key: DataWord): Option[DataWord] = ???

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 */
	override def getNonce(address: ImmutableBytes): Option[BigInt] = ???

	override def close(): Unit = ???

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 */
	override def increaseNonce(address: ImmutableBytes): Option[BigInt] = ???

	override def isClosed: Boolean = ???

	override def reset(): Unit = ???

	override def syncToRoot(root: Array[Byte]): Unit = ???

	/**
	 * 指定されたアカウントに対して、コードを保存します。
	 */
	override def saveCode(address: ImmutableBytes, code: ImmutableBytes): Unit = ???

	override def commit(): Unit = ???
}

object RepositoryImpl {
	private val DETAILS_DB: String = "details"
	private val STATE_DB: String = "state"
	private val logger: Logger = LoggerFactory.getLogger("repository")
	private val gLogger: Logger = LoggerFactory.getLogger("general")
}
