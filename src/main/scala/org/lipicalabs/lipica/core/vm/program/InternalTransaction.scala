package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.{Address, Transaction, TransactionLike}
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 15:36
 * YANAGISAWA, Kentaro
 */
class InternalTransaction(private val parentHash: DigestValue, val deep: Int, val index: Int, _nonce: ImmutableBytes, _manaPrice: DataWord, _manaLimit: DataWord, override val senderAddress: Address, _receiveAddress: Address, _value: ImmutableBytes, _data: ImmutableBytes, val note: String) extends TransactionLike {

	private val core = Transaction(_nonce, _manaPrice.data, _manaLimit.data, _receiveAddress, _value, _data)

	private var rejected = false

	override def nonce = this.core.nonce
	override def data = this.core.data
	override def manaLimit = this.core.manaLimit
	override def manaPrice = this.core.manaPrice
	override def value = this.core.value
	override def receiverAddress = this.core.receiverAddress

	def reject(): Unit = {
		this.rejected = true
	}
	def isRejected: Boolean = this.rejected

	private var rbacBytes: ImmutableBytes = null
	override def toEncodedBytes: ImmutableBytes = {
		if (this.rbacBytes eq null) {
			val thisNonce = nonce
			val encodedNonce =
				if (ByteUtils.isNullOrEmpty(thisNonce) || ((thisNonce.length == 1) && (thisNonce(0) == 0))) {
					RBACCodec.Encoder.encode(null)
				} else {
					RBACCodec.Encoder.encode(thisNonce)
				}
			val encodedSenderAddress = RBACCodec.Encoder.encode(senderAddress)
			val encodedReceiverAddress = RBACCodec.Encoder.encode(receiverAddress)
			val encodedValue = RBACCodec.Encoder.encode(value)
			val encodedManaPrice = RBACCodec.Encoder.encode(manaPrice)
			val encodedManaLimit = RBACCodec.Encoder.encode(manaLimit)
			val encodedData = RBACCodec.Encoder.encode(data)
			val encodedParentHash = RBACCodec.Encoder.encode(parentHash)
			val encodedNote = RBACCodec.Encoder.encode(note)
			val encodedDeep = RBACCodec.Encoder.encode(deep)
			val encodedIndex = RBACCodec.Encoder.encode(index)
			val encodedRejected = RBACCodec.Encoder.encode(rejected)

			this.rbacBytes = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedNonce, encodedParentHash, encodedSenderAddress, encodedReceiverAddress, encodedValue, encodedManaPrice, encodedManaLimit, encodedData, encodedNote, encodedDeep, encodedIndex, encodedRejected))
		}
		this.rbacBytes
	}

	override def toEncodedRawBytes = this.toEncodedBytes

	override def sign(privateKeyBytes: ImmutableBytes) = {
		throw new UnsupportedOperationException("Cannot sign an internal transaction.")
	}
	override val signatureOption = None
}
