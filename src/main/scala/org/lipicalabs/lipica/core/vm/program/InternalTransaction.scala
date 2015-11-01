package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.base.{Transaction, TransactionLike}
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec, ByteUtils}
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 15:36
 * YANAGISAWA, Kentaro
 */
class InternalTransaction(private val parentHash: Array[Byte], val deep: Int, val index: Int, _nonce: ImmutableBytes, _manaPrice: DataWord, _manaLimit: DataWord, override val sendAddress: ImmutableBytes, _receiveAddress: ImmutableBytes, _value: ImmutableBytes, _data: ImmutableBytes, val note: String) extends TransactionLike {

	private val core = Transaction(_nonce, _manaPrice.data, _manaLimit.data, _receiveAddress, _value, _data)

	private var rejected = false

	override def nonce = this.core.nonce
	override def data = this.core.data
	override def manaLimit = this.core.manaLimit
	override def manaPrice = this.core.manaPrice
	override def value = this.core.value
	override def receiveAddress = this.core.receiveAddress

	def reject(): Unit = {
		this.rejected = true
	}
	def isRejected: Boolean = this.rejected

	private var rbacBytes: ImmutableBytes = null
	override def encodedBytes: ImmutableBytes = {
		if (this.rbacBytes eq null) {
			val thisNonce = nonce
			val encodedNonce =
				if (ByteUtils.isNullOrEmpty(thisNonce) || ((thisNonce.length == 1) && (thisNonce(0) == 0))) {
					RBACCodec.Encoder.encode(null)
				} else {
					RBACCodec.Encoder.encode(thisNonce)
				}
			val encodedSenderAddress = RBACCodec.Encoder.encode(sendAddress)
			val encodedReceiveAddress = RBACCodec.Encoder.encode(receiveAddress)
			val encodedValue = RBACCodec.Encoder.encode(value)
			val encodedManaPrice = RBACCodec.Encoder.encode(manaPrice)
			val encodedManaLimit = RBACCodec.Encoder.encode(manaLimit)
			val encodedData = RBACCodec.Encoder.encode(data)
			val encodedParentHash = RBACCodec.Encoder.encode(parentHash)
			val encodedNote = RBACCodec.Encoder.encode(note)
			val encodedDeep = RBACCodec.Encoder.encode(deep)
			val encodedIndex = RBACCodec.Encoder.encode(index)
			val encodedRejected = RBACCodec.Encoder.encode(rejected)

			this.rbacBytes = ImmutableBytes(RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedNonce, encodedParentHash, encodedSenderAddress, encodedReceiveAddress, encodedValue, encodedManaPrice, encodedManaLimit, encodedData, encodedNote, encodedDeep, encodedIndex, encodedRejected)))
		}
		this.rbacBytes
	}

	override def encodedRawBytes = this.encodedBytes

	override def sign(privateKeyBytes: ImmutableBytes) = {
		throw new UnsupportedOperationException("Cannot sign an internal transaction.")
	}
	override val signatureOption = None
}
