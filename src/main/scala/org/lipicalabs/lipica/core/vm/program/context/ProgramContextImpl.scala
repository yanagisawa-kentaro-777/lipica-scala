package org.lipicalabs.lipica.core.vm.program.context

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.datastore.{RepositoryLike, BlockStore}
import org.lipicalabs.lipica.core.kernel.Address
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * ProgramContext の実装クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/31 10:54
 * YANAGISAWA, Kentaro
 */
class ProgramContextImpl private(
	//トランザクションもしくはコントラクトに関する情報。
	override val ownerAddress: DataWord,
	override val originAddress: DataWord,
	override val callerAddress: DataWord,
	override val balance: DataWord,
	override val manaPrice: DataWord,
	override val manaLimit: DataWord,
	override val callValue: DataWord,
	private val messageData: ImmutableBytes,
	//最終ブロックに関する情報。
	override val parentHash: DataWord,
	override val coinbase: DataWord,
	override val timestamp: DataWord,
	override val blockNumber: DataWord,
	override val difficulty: DataWord,
	override val blockManaLimit: DataWord,

	override val repository: RepositoryLike,
	override val callDepth: Int,
	override val blockStore: BlockStore,
	override val byTransaction: Boolean
) extends ProgramContext {


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
		this.messageData.copyTo(index, data, 0, size)
		DataWord(data)
	}

	/** CALLDATASIZE op. */
	override def dataSize: DataWord = {
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
		this.messageData.copyTo(offset, result, 0, length)
		ImmutableBytes(result)
	}

	override def toString: String = {
		"ProgramInvokeImpl{address=%s, origin=%s, caller=%s, balance=%s, mana=%s, manaPrice=%s, callValue=%s, messageData=%s, parentHash=%s, coinbase=%s, timestamp=%s, blockNumber=%s, difficulty=%s, blockManaLimit=%s, byTransaction=%s, callDepth=%s}".format(
			this.ownerAddress, this.originAddress, this.callerAddress, this.balance, this.manaLimit, this.manaPrice, this.callValue, this.messageData, this.parentHash, this.coinbase, this.timestamp, this.blockNumber, this.difficulty, this.blockManaLimit, this.byTransaction, this.callDepth
		)
	}

	override def equals(o: Any): Boolean = {
		try {
			o.asInstanceOf[ProgramContextImpl].toString == this.toString
		} catch {
			case any: Throwable => false
		}
	}

}

object ProgramContextImpl {

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
		blockStore: BlockStore
	): ProgramContextImpl = {
		new ProgramContextImpl(
			ownerAddress = address, originAddress = origin, callerAddress = caller, balance = balance, manaPrice = manaPrice, manaLimit = mana, callValue = callValue, messageData = messageData,
			parentHash = parentHash, coinbase = coinbase, timestamp = timestamp, blockNumber = blockNumber, difficulty = difficulty, blockManaLimit = blockManaLimit,
			repository = repository, callDepth = callDepth, blockStore = blockStore, byTransaction = false
		)
	}

	def apply(
		address: Address,
		origin: Address,
		caller: Address,
		balance: ImmutableBytes,
		manaPrice: BigIntBytes,
		txManaLimit: BigIntBytes,
		callValue: BigIntBytes,
		messageData: ImmutableBytes,
		parentHash: DigestValue,
		coinbase: Address,
		timestamp: Long,
		blockNumber: Long,
		difficulty: BigIntBytes,
		blockManaLimit: BigIntBytes,
		repository: RepositoryLike,
		blockStore: BlockStore
	): ProgramContextImpl = {
		new ProgramContextImpl(
			ownerAddress = DataWord(address.bytes), originAddress = DataWord(origin.bytes), callerAddress = DataWord(caller.bytes),
			balance = DataWord(balance), manaPrice = DataWord(manaPrice.bytes), manaLimit = DataWord(txManaLimit.bytes),
			callValue = DataWord(callValue.bytes), messageData = messageData,
			parentHash = DataWord(parentHash.bytes), coinbase = DataWord(coinbase.bytes), timestamp = DataWord(timestamp),
			blockNumber = DataWord(blockNumber), difficulty = DataWord(difficulty.bytes), blockManaLimit = DataWord(blockManaLimit.bytes),
			repository = repository, callDepth = 0, blockStore = blockStore, byTransaction = true
		)
	}
}