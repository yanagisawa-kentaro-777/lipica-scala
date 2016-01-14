package org.lipicalabs.lipica.core.vm.program

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.kernel.{AccountState, Block}
import org.lipicalabs.lipica.core.kernel.ContractDetails
import org.lipicalabs.lipica.core.datastore.RepositoryLike
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.context.ProgramContext
import org.lipicalabs.lipica.core.vm.program.listener.{ProgramListenerAware, ProgramListener}

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:22
 * YANAGISAWA, Kentaro
 */
class Storage private(private val address: DataWord, private val repository: RepositoryLike) extends RepositoryLike with ProgramListenerAware {

	private val traceListenerRef: AtomicReference[ProgramListener] = new AtomicReference[ProgramListener](null)

	def traceListener: ProgramListener = this.traceListenerRef.get
	override def setTraceListener(listener: ProgramListener): Unit = {
		this.traceListenerRef.set(listener)
	}

	override def createAccount(address: ImmutableBytes) = this.repository.createAccount(address)

	override def existsAccount(address: ImmutableBytes) = this.repository.existsAccount(address)

	override def getAccountState(address: ImmutableBytes) = this.repository.getAccountState(address)

	override def delete(address: ImmutableBytes) = {
		if (canListenTrace(address)) {
			this.traceListener.onStorageClear()
		}
		this.repository.delete(address)
	}

	override def increaseNonce(address: ImmutableBytes) = this.repository.increaseNonce(address)

	override def getNonce(address: ImmutableBytes) = this.repository.getNonce(address)

	override def getContractDetails(address: ImmutableBytes) = this.repository.getContractDetails(address)

	override def saveCode(address: ImmutableBytes, code: ImmutableBytes) = this.repository.saveCode(address, code)

	override def getCode(address: ImmutableBytes) = this.repository.getCode(address)

	override def addStorageRow(address: ImmutableBytes, key: DataWord, value: DataWord) = {
		if (canListenTrace(address)) {
			this.traceListener.onStoragePut(key, value)
		}
		this.repository.addStorageRow(address, key, value)
	}

	override def getStorageValue(address: ImmutableBytes, key: DataWord) = this.repository.getStorageValue(address, key)

	override def getStorageContent(address: ImmutableBytes, keys: Iterable[DataWord]): Map[DataWord, DataWord] = this.repository.getStorageContent(address, keys)

	override def getBalance(address: ImmutableBytes) = this.repository.getBalance(address)

	override def addBalance(address: ImmutableBytes, value: BigInt) = this.repository.addBalance(address, value)

	override def getAccountKeys = this.repository.getAccountKeys

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = {
		this.repository.dumpState(block, gasUsed, txNumber, txHash)
	}

	override def startTracking = this.repository.startTracking

	override def updateBatch(accountStates: mutable.Map[ImmutableBytes, AccountState], contractDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit  = {
		for (each <- contractDetails) {
			val (address, details) = each
			if (!canListenTrace(address)) {
				return
			}

			if (details.isDeleted) {
				this.traceListener.onStorageClear()
			} else if (details.isDirty) {
				for (entry <- details.storageContent) {
					traceListener.onStoragePut(entry._1, entry._2)
				}
			}
		}
		this.repository.updateBatch(accountStates, contractDetails)
	}

	override def loadAccount(address: ImmutableBytes, cacheAccounts: mutable.Map[ImmutableBytes, AccountState], cacheDetails: mutable.Map[ImmutableBytes, ContractDetails]): Unit = {
		this.repository.loadAccount(address, cacheAccounts, cacheDetails)
	}

	private def canListenTrace(address: ImmutableBytes): Boolean = {
		(this.address == DataWord(address)) && (traceListener != null)
	}

	override def close(): Unit = ()
}

object Storage {

	def apply(context: ProgramContext): Storage = {
		new Storage(context.getOwnerAddress, context.getRepository)
	}
}
