package org.lipicalabs.lipica.core.vm.program.context

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.datastore.{RepositoryLike, BlockStore}
import org.lipicalabs.lipica.core.kernel.Address
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.VMWord

/**
 * ProgramContext の実装クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/31 10:54
 * YANAGISAWA, Kentaro
 */
class ProgramContextImpl private(
	//トランザクションもしくはコントラクトに関する情報。
	override val ownerAddress: VMWord,
	override val originAddress: VMWord,
	override val callerAddress: VMWord,
	override val balance: VMWord,
	override val manaPrice: VMWord,
	override val manaLimit: VMWord,
	override val callValue: VMWord,
	private val messageData: ImmutableBytes,
	//最終ブロックに関する情報。
	override val parentHash: VMWord,
	override val coinbase: VMWord,
	override val timestamp: VMWord,
	override val blockNumber: VMWord,
	override val difficulty: VMWord,
	override val blockManaLimit: VMWord,

	override val repository: RepositoryLike,
	override val callDepth: Int,
	override val blockStore: BlockStore,
	override val byTransaction: Boolean
) extends ProgramContext {


	private val MaxMessageData = BigInt(Int.MaxValue)

	/** CALLDATALOAD op. */
	override def getDataValue(indexData: VMWord): VMWord = {
		val tempIndex = indexData.value
		val index = tempIndex.intValue()

		if (ByteUtils.isNullOrEmpty(this.messageData) || (this.messageData.length <= index) || (MaxMessageData < tempIndex)) {
			return VMWord.Zero
		}

		val size =
			if (this.messageData.length < (index + VMWord.NumberOfBytes)) {
				messageData.length - index
			} else {
				VMWord.NumberOfBytes
			}
		val data = new Array[Byte](VMWord.NumberOfBytes)
		this.messageData.copyTo(index, data, 0, size)
		VMWord(data)
	}

	/** CALLDATASIZE op. */
	override def dataSize: VMWord = {
		if (ByteUtils.isNullOrEmpty(this.messageData)) return VMWord.Zero
		VMWord(this.messageData.length)
	}

	/** CALLDATACOPY */
	override def getDataCopy(offsetData: VMWord, lengthData: VMWord): ImmutableBytes = {
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
		address: VMWord,
		origin: VMWord,
		caller: VMWord,
		balance: VMWord,
		manaPrice: VMWord,
		mana: VMWord,
		callValue: VMWord,
		messageData: ImmutableBytes,
		parentHash: VMWord,
		coinbase: VMWord,
		timestamp: VMWord,
		blockNumber: VMWord,
		difficulty: VMWord,
		blockManaLimit: VMWord,
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
			ownerAddress = VMWord(address.bytes), originAddress = VMWord(origin.bytes), callerAddress = VMWord(caller.bytes),
			balance = VMWord(balance), manaPrice = VMWord(manaPrice.bytes), manaLimit = VMWord(txManaLimit.bytes),
			callValue = VMWord(callValue.bytes), messageData = messageData,
			parentHash = VMWord(parentHash.bytes), coinbase = VMWord(coinbase.bytes), timestamp = VMWord(timestamp),
			blockNumber = VMWord(blockNumber), difficulty = VMWord(difficulty.bytes), blockManaLimit = VMWord(blockManaLimit.bytes),
			repository = repository, callDepth = 0, blockStore = blockStore, byTransaction = true
		)
	}
}