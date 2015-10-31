package org.lipicalabs.lipica.core.base

import java.math.BigInteger
import java.security.SignatureException

import org.apache.commons.codec.binary.Hex
import org.apache.commons.lang3.ArrayUtils
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.ECKey.ECDSASignature
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ByteUtils, RBACCodec}
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

	import Transaction._

	/* a counter used to make sure each transaction can only be processed once */
	def nonce: Array[Byte]

	def sendAddress: Array[Byte]

	/* the amount of lipica to transfer (calculated as wei) */
	def value: Array[Byte]

	/**
	 *  the address of the destination account
	 * In creation transaction the receive address is - 0
	 */
	def receiveAddress: Array[Byte]

	/**
	 * the amount of lipica to pay as a transaction fee
	 * to the miner for each unit of mana
	 */
	def manaPrice: Array[Byte]

	/**
	 * The amount of "mana" to allow for the computation.
	 * Mana is the fuel of the computational engine;
	 * every computational step taken and every byte added
	 * to the state or transaction list consumes some mana.
	 * */
	def manaLimit: Array[Byte]

	/**
	 * An unlimited size byte array specifying
	 * input [data] of the message call or
	 * Initialization code for a new contract
	 * */
	def data: Array[Byte]

	def sign(privateKeyBytes: Array[Byte]): Unit

	/**
	 *  the elliptic curve signature
	 * (including public key recovery bits)
	 * */
	def signatureOption: Option[ECDSASignature]

	def encodedBytes: Array[Byte]

	def encodedRawBytes: Array[Byte]

	/**
	 * このトランザクションにかかるマナの量を返します。
	 */
	def transactionCost: Long = {
		val nonZeroes = nonZeroDataBytes
		val zeroVals = ArrayUtils.getLength(data) - nonZeroes
		ManaCost.TRANSACTION + zeroVals * ManaCost.TX_ZERO_DATA + nonZeroes * ManaCost.TX_NO_ZERO_DATA
	}

	def hash: Array[Byte] = DigestUtils.sha3(encodedBytes)

	def rawHash: Array[Byte] = DigestUtils.sha3(encodedRawBytes)

	def getContractAddress: Array[Byte] = {
		if (!isContractCreation) return null
		DigestUtils.computeNewAddress(this.sendAddress, this.nonce)
	}

	def isContractCreation: Boolean = {
		ByteUtils.isNullOrEmpty(this.receiveAddress) || (this.receiveAddress sameElements zeroByteArray)
	}

	def getKey: Option[ECKey] = {
		val hash = rawHash
		this.signatureOption.map {
			signature => ECKey.recoverFromSignature(signature.v, signature, hash, true)
		}
	}

	override def hashCode: Int = java.util.Arrays.hashCode(this.hash)

	override def equals(o: Any): Boolean = {
		try {
			o.asInstanceOf[TransactionLike].hash sameElements this.hash
		} catch {
			case any: Throwable => false
		}
	}

	protected[base] def encode: Array[Byte] = encode(withSignature = true)

	protected[base] def encodeRaw: Array[Byte] = encode(withSignature = false)

	private def encode(withSignature: Boolean): Array[Byte] = {
		val nonce =
			if ((this.nonce eq null) || (this.nonce sameElements zeroByteArray)) {
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
}


class UnsignedTransaction(
	override val nonce: Array[Byte],
	override val value: Array[Byte],
	override val receiveAddress: Array[Byte],
	override val manaPrice: Array[Byte],
	override val manaLimit: Array[Byte],
	override val data: Array[Byte]
) extends TransactionLike {

	import Transaction._

	private var encoded: Array[Byte] = null
	override def encodedBytes: Array[Byte] = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: Array[Byte] = null
	override def encodedRawBytes: Array[Byte] = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: Array[Byte] = null
	override def sendAddress: Array[Byte] = {
		try {
			if (this._sendAddress eq null) {
				val key = ECKey.signatureToKey(rawHash, signatureOption.get.toBase64)
				this._sendAddress = key.getAddress
			}
			this._sendAddress
		} catch {
			case e: SignatureException =>
				logger.error(e.getMessage, e)
				Array.emptyByteArray
		}
	}

	private var signature: ECDSASignature = null
	def sign(privateKeyBytes: Array[Byte]): Unit = {
		val hash = this.rawHash
		val key = ECKey.fromPrivate(privateKeyBytes).decompress
		this.signature = key.sign(hash)
		this.encoded = null
	}
	override def signatureOption: Option[ECDSASignature] = Option(this.signature)

}

class SignedTransaction(
	override val nonce: Array[Byte],
	override val value: Array[Byte],
	override val receiveAddress: Array[Byte],
	override val manaPrice: Array[Byte],
	override val manaLimit: Array[Byte],
	override val data: Array[Byte],
	signature: ECDSASignature) extends TransactionLike {

	import Transaction._

	override val signatureOption: Option[ECDSASignature] = Option(signature)

	override def sign(privateKeyBytes: Array[Byte]): Unit = ()

	private var encoded: Array[Byte] = null
	override def encodedBytes: Array[Byte] = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: Array[Byte] = null
	override def encodedRawBytes: Array[Byte] = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: Array[Byte] = null
	override def sendAddress: Array[Byte] = {
		try {
			if (this._sendAddress eq null) {
				val key = ECKey.signatureToKey(rawHash, signatureOption.get.toBase64)
				this._sendAddress = key.getAddress
			}
			this._sendAddress
		} catch {
			case e: SignatureException =>
				logger.error(e.getMessage, e)
				Array.emptyByteArray
		}
	}
}

class EncodedTransaction(private val _encodedBytes: Array[Byte]) extends TransactionLike {
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
				if ((sixthElem ne null) && sixthElem.nonEmpty) {
					val v = transaction(6).bytes(0)
					val r = transaction(7).bytes
					val s = transaction(8).bytes
					val signature = ECDSASignature.fromComponents(r, s, v)
					new SignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data, signature)
				} else {
					logger.debug("RBAC encoded tx is not signed!")
					new UnsignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data)
				}
		}
		this.parsed
	}

	override def encodedBytes: Array[Byte] = {
		if (this.parsed eq null) {
			this._encodedBytes
		} else {
			parsed.encodedBytes
		}
	}

	private var encodedRaw: Array[Byte] = null
	override def encodedRawBytes: Array[Byte] = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = parse.encodeRaw
		}
		this.encodedRaw
	}

	override def sign(privateKeyBytes: Array[Byte]): Unit = parse.sign(privateKeyBytes)

	override def nonce: Array[Byte] = parse.nonce

	override def sendAddress: Array[Byte] = parse.sendAddress

	override def value: Array[Byte] = parse.value

	override def receiveAddress: Array[Byte] = parse.receiveAddress

	override def manaPrice: Array[Byte] = parse.manaPrice

	override def manaLimit: Array[Byte] = parse.manaLimit

	override def data: Array[Byte] = parse.data

	override def signatureOption: Option[ECDSASignature] = parse.signatureOption

}


object Transaction {
	val logger = LoggerFactory.getLogger(getClass)

	private val DEFAULT_MANA_PRICE = BigInt("10000000000000")
	private val DEFAULT_BALANCE_MANA = BigInt("21000")

	val zeroByteArray = Array[Byte](0)

	def apply(rawData: Array[Byte]): TransactionLike = {
		new EncodedTransaction(rawData)
	}

	def apply(nonce: Array[Byte], manaPrice: Array[Byte], manaLimit: Array[Byte], receiveAddress: Array[Byte], value: Array[Byte], data: Array[Byte]): TransactionLike = {
		new UnsignedTransaction(launderEmptyToZero(nonce), launderEmptyToZero(value), receiveAddress, launderEmptyToZero(manaPrice), manaLimit, data)
	}

	def apply(nonce: Array[Byte], manaPrice: Array[Byte], manaLimit: Array[Byte], receiveAddress: Array[Byte], value: Array[Byte], data: Array[Byte], r: Array[Byte], s: Array[Byte], v: Byte): TransactionLike = {
		val signature: ECKey.ECDSASignature = new ECKey.ECDSASignature(new BigInteger(r), new BigInteger(s))
		signature.v = v
		new SignedTransaction(launderEmptyToZero(nonce), launderEmptyToZero(value), receiveAddress, launderEmptyToZero(manaPrice), manaLimit, data, signature)
	}

	def create(to: String, amount: BigInt, nonce: BigInt, manaPrice: BigInt, manaLimit: BigInt): TransactionLike = {
		Transaction.apply(ByteUtils.asUnsignedByteArray(nonce), ByteUtils.asUnsignedByteArray(manaPrice), ByteUtils.asUnsignedByteArray(manaLimit), Hex.decodeHex(to.toCharArray), ByteUtils.asUnsignedByteArray(amount), null)
	}

	def createDefault(to: String, amount: BigInteger, nonce: BigInteger): TransactionLike = {
		create(to, amount, nonce, DEFAULT_MANA_PRICE, DEFAULT_BALANCE_MANA)
	}

	def launderEmptyToZero(bytes: Array[Byte]): Array[Byte] = {
		if ((bytes eq null) || bytes.isEmpty) {
			zeroByteArray
		} else {
			bytes
		}
	}
}
