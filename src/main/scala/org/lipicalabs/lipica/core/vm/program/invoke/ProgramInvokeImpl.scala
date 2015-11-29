package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.db.{RepositoryLike, BlockStore}
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 10:54
 * YANAGISAWA, Kentaro
 */
class ProgramInvokeImpl private(
	//トランザクションに関する情報。
	private val address: DataWord,
	private val origin: DataWord,
	private val caller: DataWord,
	private val balance: DataWord,
	private val manaPrice: DataWord,
	private val mana: DataWord,
	private val callValue: DataWord,
	private val messageData: ImmutableBytes,
	//最終ブロックに関する情報。
	private val parentHash: DataWord,
	private val coinbase: DataWord,
	private val timestamp: DataWord,
	private val blockNumber: DataWord,
	private val difficulty: DataWord,
	private val blockManaLimit: DataWord,

	private val repository: RepositoryLike,
	private val callDepth: Int,
	override val blockStore: BlockStore,
	override val byTransaction: Boolean,
	override val byTestingSuite: Boolean
) extends ProgramInvoke {

	/** ADDRESS op. */
	override def getOwnerAddress = this.address

	/** BALANCE op. */
	override def getBalance = this.balance

	/** ORIGIN op. */
	override def getOriginAddress = this.origin

	/** CALLER op. */
	override def getCallerAddress = this.caller

	/** MANAPRICE op. */
	override def getMinManaPrice = this.manaPrice

	/** MANA op. */
	override def getMana = this.mana

	/** CALLVALUE op. */
	override def getCallValue = this.callValue

	private val MaxMessageData = BigInt(Int.MaxValue)

	/** CALLDATALOAD op. */
	override def getDataValue(indexData: DataWord): DataWord = {
		val tempIndex = indexData.value
		val index = tempIndex.intValue()

		if (ByteUtils.isNullOrEmpty(this.messageData) || (this.messageData.length <= index) || (MaxMessageData < tempIndex)) {
			return DataWord.Zero
		}

		val size =
			if (this.messageData.length < (index + DataWord.NUM_BYTES)) {
				messageData.length - index
			} else {
				DataWord.NUM_BYTES
			}
		val data = new Array[Byte](DataWord.NUM_BYTES)
		System.arraycopy(this.messageData, index, data, 0, size)
		DataWord(data)
	}

	/** CALLDATASIZE op. */
	override def getDataSize: DataWord = {
		if (ByteUtils.isNullOrEmpty(this.messageData)) return DataWord.Zero
		DataWord(this.messageData.length)
	}

	/** CALLDATACOPY */
	override def getDataCopy(offsetData: DataWord, lengthData: DataWord): ImmutableBytes = {
		val offset = offsetData.intValue
		val len = lengthData.intValue

		val result = new Array[Byte](len)

		if (ByteUtils.isNullOrEmpty(messageData)) return ImmutableBytes(result)
		if (this.messageData.length <= offset) return ImmutableBytes(result)
		val length =
			if (this.messageData.length < (offset + len)) {
				this.messageData.length - offset
			} else {
				len
			}
		System.arraycopy(this.messageData, offset, result, 0, length)
		ImmutableBytes(result)
	}

	/** PREVHASH op. */
	override def getParentHash = this.parentHash

	/** COINBASE op. */
	override def getCoinbase = this.coinbase

	/** TIMESTAMP op. */
	override def getTimestamp = this.timestamp

	/** NUMBER op. */
	override def getBlockNumber = this.blockNumber

	/** DIFFICULTY op. */
	override def getDifficulty = this.difficulty

	/** MANALIMIT op. */
	override def getBlockManaLimit = this.blockManaLimit

	override def getCallDepth: Int = this.callDepth

	override def getRepository = this.repository

	override def toString: String = {
		"ProgramInvokeImpl{address=%s, origin=%s, caller=%s, balance=%s, mana=%s, manaPrice=%s, callValue=%s, messageData=%s, parentHash=%s, coinbase=%s, timestamp=%s, blockNumber=%s, difficulty=%s, blockManaLimit=%s, byTransaction=%s, byTestingSuite=%s, callDepth=%s}".format(
			this.address, this.origin, this.caller, this.balance, this.mana, this.manaPrice, this.callValue, this.messageData, this.parentHash, this.coinbase, this.timestamp, this.blockNumber, this.difficulty, this.blockManaLimit, this.byTransaction, this.byTestingSuite, this.callDepth
		)
	}

	override def equals(o: Any): Boolean = {
		try {
			o.asInstanceOf[ProgramInvokeImpl].toString == this.toString
		} catch {
			case any: Throwable => false
		}
	}

}

object ProgramInvokeImpl {

	def apply(
		address: DataWord,
		origin: DataWord,
		caller: DataWord,
		balance: DataWord,
		manaPrice: DataWord,
		mana: DataWord,
		callValue: DataWord,
		messageData: ImmutableBytes,
		parentHash: DataWord,
		coinbase: DataWord,
		timestamp: DataWord,
		blockNumber: DataWord,
		difficulty: DataWord,
		blockManaLimit: DataWord,
		repository: RepositoryLike,
		callDepth: Int,
		blockStore: BlockStore,
		byTestingSuite: Boolean
	): ProgramInvokeImpl = {
		new ProgramInvokeImpl(
			address = address, origin = origin, caller = caller, balance = balance, manaPrice = manaPrice, mana = mana, callValue = callValue, messageData = messageData,
			parentHash = parentHash, coinbase = coinbase, timestamp = timestamp, blockNumber = blockNumber, difficulty = difficulty, blockManaLimit = blockManaLimit,
			repository = repository, callDepth = callDepth, blockStore = blockStore, byTransaction = false,  byTestingSuite = byTestingSuite
		)
	}

	def apply(
		address: ImmutableBytes,
		origin: ImmutableBytes,
		caller: ImmutableBytes,
		balance: ImmutableBytes,
		manaPrice: ImmutableBytes,
		mana: ImmutableBytes,
		callValue: ImmutableBytes,
		messageData: ImmutableBytes,
		parentHash: ImmutableBytes,
		coinbase: ImmutableBytes,
		timestamp: Long,
		blockNumber: Long,
		difficulty: ImmutableBytes,
		blockManaLimit: Long,
		repository: RepositoryLike,
		blockStore: BlockStore,
		byTestingSuite: Boolean
	): ProgramInvokeImpl = {
		new ProgramInvokeImpl(
			address = DataWord(address), origin = DataWord(origin), caller = DataWord(caller),
			balance = DataWord(balance), manaPrice = DataWord(manaPrice), mana = DataWord(mana),
			callValue = DataWord(callValue), messageData = messageData,
			parentHash = DataWord(parentHash), coinbase = DataWord(coinbase), timestamp = DataWord(timestamp),
			blockNumber = DataWord(blockNumber), difficulty = DataWord(difficulty), blockManaLimit = DataWord(blockManaLimit),
			repository = repository, callDepth = 0, blockStore = blockStore, byTransaction = true, byTestingSuite = byTestingSuite
		)
	}
}