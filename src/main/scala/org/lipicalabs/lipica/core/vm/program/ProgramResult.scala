package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{CallCreate, LogInfo, DataWord}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 15:35
 * YANAGISAWA, Kentaro
 */
class ProgramResult {

	private var manaUsed: Long = 0L
	private var hReturn = ImmutableBytes.empty
	private var exception: RuntimeException = null

	private val deleteAccounts: mutable.Buffer[DataWord] = new ArrayBuffer[DataWord]
	private val internalTransactions: mutable.Buffer[InternalTransaction] = new ArrayBuffer[InternalTransaction]
	private val logInfoBuffer: mutable.Buffer[LogInfo] = new ArrayBuffer[LogInfo]

	private var futureRefund: Long = 0L

	/** テスト用。（実行せずに貯める。） */
	private val callCreateBuffer: mutable.Buffer[CallCreate] = new ArrayBuffer[CallCreate]

	def spendMana(mana: Long): Unit = {
		this.manaUsed += mana
	}

	def refundMana(mana: Long): Unit = {
		this.manaUsed -= mana
	}

	def addFutureRefund(v: Long): Unit = {
		this.futureRefund += v
	}
	def resetFutureRefund(): Unit = {
		this.futureRefund = 0
	}
	def getFutureRefund: Long = this.futureRefund

	def setHReturn(v: ImmutableBytes): Unit = {
		this.hReturn = v
	}

	def getHReturn: ImmutableBytes = this.hReturn

	def getDeleteAccounts: Seq[DataWord] = this.deleteAccounts.toSeq
	def addDeleteAccount(address: DataWord): Unit = {
		this.deleteAccounts.append(address)
	}
	def addDeleteAccounts(accounts: Iterable[DataWord]): Unit = {
		this.deleteAccounts ++= accounts
	}

	def getLogInfoList: Seq[LogInfo] = this.logInfoBuffer.toSeq
	def addLogInfo(info: LogInfo): Unit = {
		this.logInfoBuffer.append(info)
	}
	def addLogInfos(infos: Iterable[LogInfo]): Unit = {
		this.logInfoBuffer ++= infos
	}

	def getCallCreateList: Seq[CallCreate] = this.callCreateBuffer.toSeq
	def addCallCreate(data: ImmutableBytes, destination: ImmutableBytes, manaLimit: ImmutableBytes, value: ImmutableBytes): Unit = {
		this.callCreateBuffer.append(CallCreate(data.toByteArray, destination.toByteArray, manaLimit.toByteArray, value.toByteArray))
	}

	def getInternalTransactions: Seq[InternalTransaction] = this.internalTransactions.toSeq
	def addInternalTransaction(parentHash: ImmutableBytes, deep: Int, nonce: ImmutableBytes, manaPrice: DataWord, manaLimit: DataWord, senderAddress: ImmutableBytes, receiveAddress: ImmutableBytes, value: ImmutableBytes, data: ImmutableBytes, note: String): InternalTransaction = {
		val index = this.internalTransactions.size
		val result = new InternalTransaction(parentHash.toByteArray, deep, index, nonce, manaPrice, manaLimit, senderAddress, receiveAddress, value, data, note)
		this.internalTransactions.append(result)
		result
	}
	def addInternalTransactions(transactions: Iterable[InternalTransaction]): Unit = {
		this.internalTransactions ++= transactions
	}
	def rejectInternalTransactions(): Unit = {
		this.internalTransactions.foreach(_.reject())
	}

	def mergeToThis(another: ProgramResult): Unit = {
		addInternalTransactions(another.getInternalTransactions)
		addDeleteAccounts(another.getDeleteAccounts)
		addLogInfos(another.getLogInfoList)
		addFutureRefund(another.getFutureRefund)
	}

}

object ProgramResult {

	def createEmpty: ProgramResult = {
		val result = new ProgramResult
		result.setHReturn(ImmutableBytes.empty)
		result
	}
}