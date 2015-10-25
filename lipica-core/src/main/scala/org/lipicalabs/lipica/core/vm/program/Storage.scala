package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.base.{AccountState, Block, Repository}
import org.lipicalabs.lipica.core.db.ContractDetails
import org.lipicalabs.lipica.core.utils.ByteArrayWrapper
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvoke
import org.lipicalabs.lipica.core.vm.program.listener.{ProgramListenerAware, ProgramListener}

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:22
 * YANAGISAWA, Kentaro
 */
class Storage private(private val address: DataWord, private val repository: Repository) extends Repository with ProgramListenerAware {

	private var traceListener: ProgramListener = null

	override def setTraceListener(traceListener: ProgramListener): Unit = {
		this.traceListener = traceListener
	}

	override def createAccount(address: Array[Byte]) = this.repository.createAccount(address)

	override def existsAccount(address: Array[Byte]) = this.repository.existsAccount(address)

	override def getAccountState(address: Array[Byte]) = this.repository.getAccountState(address)

	override def delete(address: Array[Byte]) = {
		if (canListenTrace(address)) {
			this.traceListener.onStorageClear()
		}
		this.repository.delete(address)
	}

	override def increaseNonce(address: Array[Byte]) = this.repository.increaseNonce(address)

	override def getNonce(address: Array[Byte]) = this.repository.getNonce(address)

	override def getContractDetails(address: Array[Byte]) = this.repository.getContractDetails(address)

	override def saveCode(address: Array[Byte], code: Array[Byte]) = this.repository.saveCode(address, code)

	override def getCode(address: Array[Byte]) = this.repository.getCode(address)

	override def addStorageRow(address: Array[Byte], key: DataWord, value: DataWord) = {
		if (canListenTrace(address)) {
			this.traceListener.onStoragePut(key, value)
		}
		this.repository.addStorageRow(address, key, value)
	}

	override def getStorageValue(address: Array[Byte], key: DataWord) = this.repository.getStorageValue(address, key)

	override def getBalance(address: Array[Byte]) = this.repository.getBalance(address)

	override def addBalance(address: Array[Byte], value: BigInt) = this.repository.addBalance(address, value)

	override def getAccountKeys = this.repository.getAccountKeys

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: Array[Byte]) = {
		this.repository.dumpState(block, gasUsed, txNumber, txHash)
	}

	override def startTracking = this.repository.startTracking

	override def flush() = this.repository.flush()

	override def flushNoReconnect() = {
		throw new UnsupportedOperationException
	}

	override def commit() = this.repository.commit()

	override def rollback() = this.repository.rollback()

	override def syncToRoot(root: Array[Byte]) = this.repository.syncToRoot(root)

	override def close() = this.repository.close()

	override def isClosed = this.repository.isClosed

	override def reset() = this.repository.reset()

	override def getRoot = this.repository.getRoot

	override def updateBatch(accountStates: Map[ByteArrayWrapper, AccountState], contractDetails: Map[ByteArrayWrapper, ContractDetails]) = {
		//TODO tracelistener への記録が未実装。
		this.repository.updateBatch(accountStates, contractDetails)
	}

	override def loadAccount(address: Array[Byte], cacheAccounts: Map[ByteArrayWrapper, AccountState], cacheDetails: Map[ByteArrayWrapper, ContractDetails]) = {
		this.repository.loadAccount(address, cacheAccounts, cacheDetails)
	}

	override def getSnapshotTo(root: Array[Byte]) = {
		throw new UnsupportedOperationException
	}

	private def canListenTrace(address: Array[Byte]): Boolean = {
		(this.address == DataWord(address)) && (traceListener != null)
	}
}

object Storage {

	def apply(programInvoke: ProgramInvoke): Storage = {
		new Storage(programInvoke.getOwnerAddress, programInvoke.getRepository)
	}
}
