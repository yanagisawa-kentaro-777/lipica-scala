package org.lipicalabs.lipica.core.vm.program

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.kernel.{Address, AccountState, Block, ContractDetails}
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

	override def createAccount(address: Address) = this.repository.createAccount(address)

	override def existsAccount(address: Address) = this.repository.existsAccount(address)

	override def getAccountState(address: Address) = this.repository.getAccountState(address)

	override def delete(address: Address) = {
		if (canListenTrace(address)) {
			this.traceListener.onStorageClear()
		}
		this.repository.delete(address)
	}

	override def increaseNonce(address: Address) = this.repository.increaseNonce(address)

	override def getNonce(address: Address) = this.repository.getNonce(address)

	override def getContractDetails(address: Address) = this.repository.getContractDetails(address)

	override def saveCode(address: Address, code: ImmutableBytes) = this.repository.saveCode(address, code)

	override def getCode(address: Address) = this.repository.getCode(address)

	override def addStorageRow(address: Address, key: DataWord, value: DataWord) = {
		if (canListenTrace(address)) {
			this.traceListener.onStoragePut(key, value)
		}
		this.repository.addStorageRow(address, key, value)
	}

	override def getStorageValue(address: Address, key: DataWord) = this.repository.getStorageValue(address, key)

	override def getStorageContent(address: Address, keys: Iterable[DataWord]): Map[DataWord, DataWord] = this.repository.getStorageContent(address, keys)

	override def getBalance(address: Address) = this.repository.getBalance(address)

	override def addBalance(address: Address, value: BigInt) = this.repository.addBalance(address, value)

	override def getAccountKeys = this.repository.getAccountKeys

	override def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes) = {
		this.repository.dumpState(block, gasUsed, txNumber, txHash)
	}

	override def startTracking = this.repository.startTracking

	override def updateBatch(accountStates: mutable.Map[Address, AccountState], contractDetails: mutable.Map[Address, ContractDetails]): Unit  = {
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

	override def loadAccount(address: Address, cacheAccounts: mutable.Map[Address, AccountState], cacheDetails: mutable.Map[Address, ContractDetails]): Unit = {
		this.repository.loadAccount(address, cacheAccounts, cacheDetails)
	}

	private def canListenTrace(address: Address): Boolean = {
		(this.address == DataWord(address.bytes)) && (traceListener != null)
	}

	override def close(): Unit = ()
}

object Storage {

	def apply(context: ProgramContext): Storage = {
		new Storage(context.getOwnerAddress, context.getRepository)
	}
}
