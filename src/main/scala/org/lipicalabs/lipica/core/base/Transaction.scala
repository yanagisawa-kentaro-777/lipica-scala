package org.lipicalabs.lipica.core.base

import java.math.BigInteger
import java.security.SignatureException

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.ECKey.ECDSASignature
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils, RBACCodec}
import org.lipicalabs.lipica.core.vm.ManaCost
import org.slf4j.LoggerFactory


/**
 * トランザクションは、このシステムの外部の行為者によって
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

	/**
	 * 送信者がこれまで送ったトランザクションの数と等しい連番。
	 */
	def nonce: ImmutableBytes

	/**
	 * 送信者のアドレス。
	 */
	def senderAddress: ImmutableBytes

	/**
	 * メッセージコールにおいては、受信者が受け取る金額。
	 * コントラクト作成においては、作成されたコントラクトアカウントへの寄託額。
	 */
	def value: ImmutableBytes

	/**
	 *  受信者のアドレス。
	 */
	def receiverAddress: ImmutableBytes

	/**
	 * マナ１単位を調達するのに必要な金額。
	 */
	def manaPrice: ImmutableBytes

	/**
	 * このトランザクションで消費して良いマナの最大量。
	 * 先払いとして引き当てられる。
	 */
	def manaLimit: ImmutableBytes

	/**
	 * メッセージコールの場合、コールに伴う入力値。
	 * コントラクト作成の場合、初期実行コード。
	 */
	def data: ImmutableBytes

	def sign(privateKeyBytes: ImmutableBytes): Unit

	/**
	 *  the elliptic curve signature
	 * (including public key recovery bits)
	 * */
	def signatureOption: Option[ECDSASignature]

	/**
	 * 署名を含めてエンコードします。
	 */
	def toEncodedBytes: ImmutableBytes

	/**
	 * 署名を含めてエンコードされたバイト列のダイジェスト値を返します。
	 */
	def hash: ImmutableBytes = toEncodedBytes.digest256

	/**
	 * 署名を含めずにエンコードします。
	 */
	def toEncodedRawBytes: ImmutableBytes

	/**
	 * 署名を含めずにエンコードされたバイト列のダイジェスト値を返します。
	 */
	def rawHash: ImmutableBytes = toEncodedRawBytes.digest256

	/**
	 * このトランザクションにかかるマナの量を返します。
	 */
	def transactionCost: Long = {
		val nonZeroes = nonZeroDataBytes
		val zeroVals = data.length - nonZeroes
		ManaCost.TRANSACTION + zeroVals * ManaCost.TX_ZERO_DATA + nonZeroes * ManaCost.TX_NO_ZERO_DATA
	}

	/**
	 * このトランザクションが、コントラクト作成用トランザクションであるか否かを返します。
	 */
	def isContractCreation: Boolean = {
		this.receiverAddress.isEmpty || (this.receiverAddress == ImmutableBytes.zero)
	}

	/**
	 * このトランザクションがコントラクト作成用のものだった場合に、
	 * そのコントラクト用のアドレスを生成して返します。
	 * メッセージコール用のトランザクションだった場合、Noneを返します。
	 */
	def contractAddress: Option[ImmutableBytes] = {
		if (!isContractCreation) return None
		Some(DigestUtils.computeNewAddress(this.senderAddress, this.nonce))
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

	override final def toString: String = {
		"Tx [Hash=%s; Nonce=%,d; ManaPrice=%,d; ManaLimit=%,d; Sender=%s; Receiver=%s; Value=%,d; Data=%s; Signature=%s]".format(
			this.hash, this.nonce.toPositiveBigInt, this.manaPrice.toPositiveBigInt, this.manaLimit.toPositiveBigInt,
			this.senderAddress, this.receiverAddress, this.value.toPositiveBigInt, this.data, this.signatureOption.map(sig => "V(%d) R(%d) S(%d)".format(sig.v, sig.r, sig.s)).getOrElse("")
		)
	}

	def summaryString: String = {
		"Tx [Hash=%s; Nonce=%,d; Sender=%s; Receiver=%s; Value=%,d; Data=%s]".format(
			this.hash.toShortString, this.nonce.toPositiveBigInt, this.senderAddress, this.receiverAddress, this.value.toPositiveBigInt, this.data
		)
	}

	protected def encode: ImmutableBytes = encode(withSignature = true)

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
		val receiveAddress = RBACCodec.Encoder.encode(this.receiverAddress)
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
	override val nonce: ImmutableBytes,
	override val value: ImmutableBytes,
	override val receiverAddress: ImmutableBytes,
	override val manaPrice: ImmutableBytes,
	override val manaLimit: ImmutableBytes,
	override val data: ImmutableBytes
) extends TransactionLike {

	import Transaction._

	private var encoded: ImmutableBytes = null
	override def toEncodedBytes: ImmutableBytes = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: ImmutableBytes = null
	override def toEncodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: ImmutableBytes = null
	override def senderAddress: ImmutableBytes = {
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
	override val receiverAddress: ImmutableBytes,
	override val manaPrice: ImmutableBytes,
	override val manaLimit: ImmutableBytes,
	override val data: ImmutableBytes,
	signature: ECDSASignature) extends TransactionLike {

	import Transaction._

	override val signatureOption: Option[ECDSASignature] = Option(signature)

	override def sign(privateKeyBytes: ImmutableBytes): Unit = ()

	private var encoded: ImmutableBytes = null
	override def toEncodedBytes: ImmutableBytes = {
		if (this.encoded eq null) {
			this.encoded = encode
		}
		this.encoded
	}

	private var encodedRaw: ImmutableBytes = null
	override def toEncodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = encodeRaw
		}
		this.encodedRaw
	}

	private var _sendAddress: ImmutableBytes = null
	override def senderAddress: ImmutableBytes = {
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

class EncodedTransaction(private val items: Seq[DecodedResult]) extends TransactionLike {
	import Transaction._

	private var parsed: TransactionLike = null
	private def parse: TransactionLike = {
		if (this.parsed eq null) {
			//val transaction = RBACCodec.Decoder.decode(this.encodedBytes).right.get.items
			//val transaction = decodedTxList.items.head.items

			val nonce = launderEmptyToZero(items.head.bytes)
			val manaPrice = launderEmptyToZero(items(1).bytes)
			val manaLimit = items(2).bytes
			val receiveAddress = items(3).bytes
			val value = launderEmptyToZero(items(4).bytes)
			val data = items(5).bytes
			val sixthElem = items(6).bytes
			this.parsed =
				if (!ByteUtils.isNullOrEmpty(sixthElem)) {
					val v = items(6).bytes(0)
					val r = items(7).bytes
					val s = items(8).bytes
					val signature = ECDSASignature.fromComponents(r.toByteArray, s.toByteArray, v)
					new SignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data, signature)
				} else {
					logger.debug("RBAC encoded tx is not signed!")
					new UnsignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data)
				}
		}
		this.parsed
	}

	override def toEncodedBytes: ImmutableBytes = {
		parse.toEncodedBytes
	}

	private var encodedRaw: ImmutableBytes = null
	override def toEncodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = parse.encodeRaw
		}
		this.encodedRaw
	}

	override def sign(privateKeyBytes: ImmutableBytes): Unit = parse.sign(privateKeyBytes)

	override def nonce: ImmutableBytes = parse.nonce

	override def senderAddress: ImmutableBytes = parse.senderAddress

	override def value: ImmutableBytes = parse.value

	override def receiverAddress: ImmutableBytes = parse.receiverAddress

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
		new EncodedTransaction(RBACCodec.Decoder.decode(rawData).right.get.items)
	}

	def decode(items: Seq[RBACCodec.Decoder.DecodedResult]): TransactionLike = {
		new EncodedTransaction(items)
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
