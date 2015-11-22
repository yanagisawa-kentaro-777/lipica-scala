package org.lipicalabs.lipica.core.base

import java.math.BigInteger
import java.security.SignatureException

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.ECKey.ECDSASignature
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils, RBACCodec}
import org.lipicalabs.lipica.core.vm.ManaCost
import org.slf4j.LoggerFactory


/**
 * トランザクションは、Lipicaシステムの外部の行為者によって
 * 送信された、署名付きの指示です。
 *
 * トランザクションには２種類あります。
 * １つは、メッセージコールを実行するもの。
 * もうひとつは、新たなコントラクトを生成するものです。
 *
 * @since 2015/10/25
 * @author YANAGISAWA, Kentaro
 */
trait TransactionLike {

	/* a counter used to make sure each transaction can only be processed once */
	def nonce: ImmutableBytes

	def sendAddress: ImmutableBytes

	/* the amount of lipica to transfer (calculated as wei) */
	def value: ImmutableBytes

	/**
	 *  the address of the destination account
	 * In creation transaction the receive address is - 0
	 */
	def receiveAddress: ImmutableBytes

	/**
	 * the amount of lipica to pay as a transaction fee
	 * to the miner for each unit of mana
	 */
	def manaPrice: ImmutableBytes

	/**
	 * The amount of "mana" to allow for the computation.
	 * Mana is the fuel of the computational engine;
	 * every computational step taken and every byte added
	 * to the state or transaction list consumes some mana.
	 * */
	def manaLimit: ImmutableBytes

	/**
	 * An unlimited size byte array specifying
	 * input [data] of the message call or
	 * Initialization code for a new contract
	 * */
	def data: ImmutableBytes

	def sign(privateKeyBytes: ImmutableBytes): Unit

	/**
	 *  the elliptic curve signature
	 * (including public key recovery bits)
	 * */
	def signatureOption: Option[ECDSASignature]

	def encodedBytes: ImmutableBytes

	def encodedRawBytes: ImmutableBytes

	/**
	 * このトランザクションにかかるマナの量を返します。
	 */
	def transactionCost: Long = {
		val nonZeroes = nonZeroDataBytes
		val zeroVals = data.length - nonZeroes
		ManaCost.TRANSACTION + zeroVals * ManaCost.TX_ZERO_DATA + nonZeroes * ManaCost.TX_NO_ZERO_DATA
	}

	def hash: ImmutableBytes = encodedBytes.digest256

	def rawHash: ImmutableBytes = encodedRawBytes.digest256

	def getContractAddress: ImmutableBytes = {
		if (!isContractCreation) return null
		DigestUtils.computeNewAddress(this.sendAddress, this.nonce)
	}

	def isContractCreation: Boolean = {
		this.receiveAddress.isEmpty || (this.receiveAddress == ImmutableBytes.zero)
	}

	def getKey: Option[ECKey] = {
		val hash = rawHash
		this.signatureOption.map {
			signature => ECKey.recoverFromSignature(signature.v, signature, hash.toByteArray, true)
		}
	}

	override def hashCode: Int = this.hash.hashCode

	override def equals(o: Any): Boolean = {
		try {
			o.asInstanceOf[TransactionLike].hash == this.hash
		} catch {
			case any: Throwable => false
		}
	}

	protected[base] def encode: ImmutableBytes = encode(withSignature = true)

	protected[base] def encodeRaw: ImmutableBytes = encode(withSignature = false)

	private def encode(withSignature: Boolean): ImmutableBytes = {
		val nonce =
			if ((this.nonce eq null) || (this.nonce == ImmutableBytes.zero)) {
				RBACCodec.Encoder.encode(null)
			} else {
				RBACCodec.Encoder.encode(this.nonce)
			}
		val manaPrice = RBACCodec.Encoder.encode(this.manaPrice)
		val manaLimit = RBACCodec.Encoder.encode(this.manaLimit)
		val receiveAddress = RBACCodec.Encoder.encode(this.receiveAddress)
		val value = RBACCodec.Encoder.encode(this.value)
		val data = RBACCodec.Encoder.encode(this.data)

		if (withSignature) {
			val (v, r, s) =
				signatureOption match {
					case Some(signature) =>
						val v = RBACCodec.Encoder.encode(signature.v)
						val r = RBACCodec.Encoder.encode(ByteUtils.asUnsignedByteArray(signature.r))
						val s = RBACCodec.Encoder.encode(ByteUtils.asUnsignedByteArray(signature.s))
						(v, r, s)
					case _ =>
						val v = RBACCodec.Encoder.encode(Array.emptyByteArray)
						val r = RBACCodec.Encoder.encode(Array.emptyByteArray)
						val s = RBACCodec.Encoder.encode(Array.emptyByteArray)
						(v, r, s)
				}
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, value, data, v, r, s))
		} else {
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, value, data))
		}
	}

	private def nonZeroDataBytes: Int = {
		if (ByteUtils.isNullOrEmpty(data)) return 0
		this.data.count(each => each != 0)
	}

	override final def toString: String = {
		"[Sender: %s, Receiver: %s, Value: %s]".format(this.sendAddress, this.receiveAddress, this.value)
	}
}


class UnsignedTransaction(
	override val nonce: ImmutableBytes,
	override val value: ImmutableBytes,
	override val receiveAddress: ImmutableBytes,
	override val manaPrice: ImmutableBytes,
	override val manaLimit: ImmutableBytes,
	override val data: ImmutableBytes
) extends TransactionLike {

	import Transaction._

	private var encoded: ImmutableBytes = null
	override def encodedBytes: ImmutableBytes = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: ImmutableBytes = null
	override def encodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: ImmutableBytes = null
	override def sendAddress: ImmutableBytes = {
		try {
			if (this._sendAddress eq null) {
				val key = ECKey.signatureToKey(rawHash.toByteArray, signatureOption.get.toBase64)
				this._sendAddress = ImmutableBytes(key.getAddress)
			}
			this._sendAddress
		} catch {
			case e: SignatureException =>
				logger.error(e.getMessage, e)
				ImmutableBytes.empty
		}
	}

	private var signature: ECDSASignature = null
	def sign(privateKeyBytes: ImmutableBytes): Unit = {
		val hash = this.rawHash
		val key = ECKey.fromPrivate(privateKeyBytes.toByteArray).decompress
		this.signature = key.sign(hash.toByteArray)
		this.encoded = null
	}
	override def signatureOption: Option[ECDSASignature] = Option(this.signature)

}

class SignedTransaction(
	override val nonce: ImmutableBytes,
	override val value: ImmutableBytes,
	override val receiveAddress: ImmutableBytes,
	override val manaPrice: ImmutableBytes,
	override val manaLimit: ImmutableBytes,
	override val data: ImmutableBytes,
	signature: ECDSASignature) extends TransactionLike {

	import Transaction._

	override val signatureOption: Option[ECDSASignature] = Option(signature)

	override def sign(privateKeyBytes: ImmutableBytes): Unit = ()

	private var encoded: ImmutableBytes = null
	override def encodedBytes: ImmutableBytes = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: ImmutableBytes = null
	override def encodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: ImmutableBytes = null
	override def sendAddress: ImmutableBytes = {
		try {
			if (this._sendAddress eq null) {
				val key = ECKey.signatureToKey(rawHash.toByteArray, signatureOption.get.toBase64)
				this._sendAddress = ImmutableBytes(key.getAddress)
			}
			this._sendAddress
		} catch {
			case e: SignatureException =>
				logger.error(e.getMessage, e)
				ImmutableBytes.empty
		}
	}
}

class EncodedTransaction(private val _encodedBytes: ImmutableBytes) extends TransactionLike {
	import Transaction._

	private var parsed: TransactionLike = null
	private def parse: TransactionLike = {
		if (this.parsed eq null) {
			val transaction = RBACCodec.Decoder.decode(this.encodedBytes).right.get.items
			//val transaction = decodedTxList.items.head.items

			val nonce = launderEmptyToZero(transaction.head.bytes)
			val manaPrice = launderEmptyToZero(transaction(1).bytes)
			val manaLimit = transaction(2).bytes
			val receiveAddress = transaction(3).bytes
			val value = launderEmptyToZero(transaction(4).bytes)
			val data = transaction(5).bytes
			val sixthElem = transaction(6).bytes
			this.parsed =
				if (!ByteUtils.isNullOrEmpty(sixthElem)) {
					val v = transaction(6).bytes(0)
					val r = transaction(7).bytes
					val s = transaction(8).bytes
					val signature = ECDSASignature.fromComponents(r.toByteArray, s.toByteArray, v)
					new SignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data, signature)
				} else {
					logger.debug("RBAC encoded tx is not signed!")
					new UnsignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data)
				}
		}
		this.parsed
	}

	override def encodedBytes: ImmutableBytes = {
		if (this.parsed eq null) {
			this._encodedBytes
		} else {
			parsed.encodedBytes
		}
	}

	private var encodedRaw: ImmutableBytes = null
	override def encodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = parse.encodeRaw
		}
		this.encodedRaw
	}

	override def sign(privateKeyBytes: ImmutableBytes): Unit = parse.sign(privateKeyBytes)

	override def nonce: ImmutableBytes = parse.nonce

	override def sendAddress: ImmutableBytes = parse.sendAddress

	override def value: ImmutableBytes = parse.value

	override def receiveAddress: ImmutableBytes = parse.receiveAddress

	override def manaPrice: ImmutableBytes = parse.manaPrice

	override def manaLimit: ImmutableBytes = parse.manaLimit

	override def data: ImmutableBytes = parse.data

	override def signatureOption: Option[ECDSASignature] = parse.signatureOption

}


object Transaction {
	val logger = LoggerFactory.getLogger(getClass)

	private val DEFAULT_MANA_PRICE = BigInt("10000000000000")
	private val DEFAULT_BALANCE_MANA = BigInt("21000")

	def decode(rawData: ImmutableBytes): TransactionLike = {
		new EncodedTransaction(rawData)
	}

	def apply(nonce: ImmutableBytes, manaPrice: ImmutableBytes, manaLimit: ImmutableBytes, receiveAddress: ImmutableBytes, value: ImmutableBytes, data: ImmutableBytes): TransactionLike = {
		new UnsignedTransaction(launderEmptyToZero(nonce), launderEmptyToZero(value), receiveAddress, launderEmptyToZero(manaPrice), manaLimit, data)
	}

	def apply(nonce: ImmutableBytes, manaPrice: ImmutableBytes, manaLimit: ImmutableBytes, receiveAddress: ImmutableBytes, value: ImmutableBytes, data: ImmutableBytes, r: ImmutableBytes, s: ImmutableBytes, v: Byte): TransactionLike = {
		val signature: ECKey.ECDSASignature = new ECKey.ECDSASignature(r.toSignedBigInteger, s.toSignedBigInteger)
		signature.v = v
		new SignedTransaction(launderEmptyToZero(nonce), launderEmptyToZero(value), receiveAddress, launderEmptyToZero(manaPrice), manaLimit, data, signature)
	}

	def create(to: String, amount: BigInt, nonce: BigInt, manaPrice: BigInt, manaLimit: BigInt): TransactionLike = {
		Transaction.apply(ImmutableBytes.asUnsignedByteArray(nonce), ImmutableBytes.asUnsignedByteArray(manaPrice), ImmutableBytes.asUnsignedByteArray(manaLimit), ImmutableBytes.parseHexString(to), ImmutableBytes.asUnsignedByteArray(amount), ImmutableBytes.empty)
	}

	def createDefault(to: String, amount: BigInteger, nonce: BigInteger): TransactionLike = {
		create(to, amount, nonce, DEFAULT_MANA_PRICE, DEFAULT_BALANCE_MANA)
	}

	def launderEmptyToZero(bytes: ImmutableBytes): ImmutableBytes = {
		if ((bytes eq null) || bytes.isEmpty) {
			ImmutableBytes.zero
		} else {
			bytes
		}
	}

}
