package org.lipicalabs.lipica.core.kernel

import java.math.BigInteger

import org.lipicalabs.lipica.core.crypto.elliptic_curve.{ECKeyPair, ECPublicKey, ECDSASignature}
import org.lipicalabs.lipica.core.crypto.digest.{DigestValue, DigestUtils}
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes, ByteUtils}
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
	def nonce: BigIntBytes

	/**
	 * 送信者のアドレス。
	 */
	def senderAddress: Address

	/**
	 * メッセージコールにおいては、受信者が受け取る金額。
	 * コントラクト作成においては、作成されたコントラクトアカウントへの寄託額。
	 */
	def value: BigIntBytes

	/**
	 *  受信者のアドレス。
	 */
	def receiverAddress: Address

	/**
	 * マナ１単位を調達するのに必要な金額。
	 */
	def manaPrice: BigIntBytes

	/**
	 * このトランザクションで消費して良いマナの最大量。
	 * 先払いとして引き当てられる。
	 */
	def manaLimit: BigIntBytes

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
	def hash: DigestValue = toEncodedBytes.digest256

	/**
	 * 署名を含めずにエンコードします。
	 */
	def toEncodedRawBytes: ImmutableBytes

	/**
	 * 署名を含めずにエンコードされたバイト列のダイジェスト値を返します。
	 */
	def rawHash: DigestValue = toEncodedRawBytes.digest256

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
	def isContractCreation: Boolean = this.receiverAddress.isEmpty

	/**
	 * このトランザクションがコントラクト作成用のものだった場合に、
	 * そのコントラクト用のアドレスを生成して返します。
	 * メッセージコール用のトランザクションだった場合、Noneを返します。
	 */
	def contractAddress: Option[Address] = {
		if (!isContractCreation) return None
		Some(DigestUtils.computeNewAddress(this.senderAddress, this.nonce))
	}

	def getKey: Option[ECPublicKey] = {
		val hash = rawHash
		this.signatureOption.flatMap {
			signature => ECPublicKey.recoverFromSignature(signature.v, signature, hash.toByteArray, compressed = true)
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
		"Tx[Hash=%s; Nonce=%,d; ManaPrice=%,d; ManaLimit=%,d; Sender=%s; Receiver=%s; Value=%s; Data=%s; Signature=%s]".format(
			this.hash, this.nonce.positiveBigInt, this.manaPrice.positiveBigInt, this.manaLimit.positiveBigInt,
			this.senderAddress, this.receiverAddress, this.value, this.data, this.signatureOption.map(sig => "V(%d) R(%d) S(%d)".format(sig.v, sig.r, sig.s)).getOrElse("")
		)
	}

	def summaryString: String = {
		"Tx[Hash=%s; Nonce=%,d; Sender=%s; Receiver=%s; Value=%s; ManaLimit=%,d; ManaPrice=%,d; Data=%s]".format(
			this.hash.toShortString, this.nonce.positiveBigInt, this.senderAddress, this.receiverAddress, this.value, this.manaLimit.positiveBigInt, this.manaPrice.positiveBigInt, this.data.toShortString
		)
	}

	protected def encode: ImmutableBytes = encode(withSignature = true)

	protected[kernel] def encodeRaw: ImmutableBytes = encode(withSignature = false)

	private def encode(withSignature: Boolean): ImmutableBytes = {
		val nonce = RBACCodec.Encoder.encode(this.nonce)
		val manaPrice = RBACCodec.Encoder.encode(this.manaPrice)
		val manaLimit = RBACCodec.Encoder.encode(this.manaLimit)
		val receiveAddress = RBACCodec.Encoder.encode(this.receiverAddress)
		val encodedValue = RBACCodec.Encoder.encode(this.value)
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
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, encodedValue, data, v, r, s))
		} else {
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, encodedValue, data))
		}
	}

	private def nonZeroDataBytes: Int = {
		if (ByteUtils.isNullOrEmpty(data)) return 0
		this.data.count(each => each != 0)
	}

}


class UnsignedTransaction(
	override val nonce: BigIntBytes,
	override val value: BigIntBytes,
	override val receiverAddress: Address,
	override val manaPrice: BigIntBytes,
	override val manaLimit: BigIntBytes,
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

	private var _sendAddress: Address = null
	override def senderAddress: Address = {
		try {
			if (this._sendAddress eq null) {
				val key = ECPublicKey.recoverFromSignature(rawHash.toByteArray, signatureOption.get.toBase64).get
				this._sendAddress = key.toAddress
			}
			this._sendAddress
		} catch {
			case e: Exception =>
				logger.error(e.getMessage, e)
				EmptyAddress
		}
	}

	private var signature: ECDSASignature = null
	def sign(privateKeyBytes: ImmutableBytes): Unit = {
		val hash = this.rawHash
		val key = ECKeyPair.fromPrivateKey(privateKeyBytes.toByteArray).decompress
		this.signature = key.sign(hash.toByteArray)
		this.encoded = null
	}
	override def signatureOption: Option[ECDSASignature] = Option(this.signature)

}

class SignedTransaction(
	override val nonce: BigIntBytes,
	override val value: BigIntBytes,
	override val receiverAddress: Address,
	override val manaPrice: BigIntBytes,
	override val manaLimit: BigIntBytes,
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

	private var _sendAddress: Address = null
	override def senderAddress: Address = {
		try {
			if (this._sendAddress eq null) {
				val key = ECPublicKey.recoverFromSignature(rawHash.toByteArray, signatureOption.get.toBase64).get
				this._sendAddress = key.toAddress
			}
			this._sendAddress
		} catch {
			case e: Exception =>
				logger.error(e.getMessage, e)
				EmptyAddress
		}
	}
}

class EncodedTransaction(private var encodedBytes: ImmutableBytes, private val items: Seq[DecodedResult]) extends TransactionLike {
	import Transaction._

	private var parsed: TransactionLike = null
	private def parse: TransactionLike = {
		if (this.parsed eq null) {
			//val transaction = RBACCodec.Decoder.decode(this.encodedBytes).right.get.items
			//val transaction = decodedTxList.items.head.items

			val nonce = BigIntBytes(items.head.bytes)
			val manaPrice = BigIntBytes(items(1).bytes)
			val manaLimit = BigIntBytes(items(2).bytes)
			val receiveAddress =
				if (items(3).bytes.isEmpty) {
					//コントラクトの生成。
					EmptyAddress
				} else {
					Address160(items(3).bytes)
				}
			val value = BigIntBytes(items(4).bytes)
			val data = items(5).bytes
			val sixthElem = items(6).bytes
			this.parsed =
				if (!ByteUtils.isNullOrEmpty(sixthElem)) {
					val v = items(6).bytes(0)
					val r = items(7).bytes
					val s = items(8).bytes
					val signature = ECDSASignature(r.toByteArray, s.toByteArray, v)
					new SignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data, signature)
				} else {
					logger.debug("RBAC encoded tx is not signed!")
					new UnsignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data)
				}
		}
		this.parsed
	}

	override def toEncodedBytes: ImmutableBytes = {
		Option(this.encodedBytes).getOrElse(parse.toEncodedBytes)
	}

	private var encodedRaw: ImmutableBytes = null
	override def toEncodedRawBytes: ImmutableBytes = {
		if (this.encodedRaw eq null) {
			this.encodedRaw = parse.encodeRaw
		}
		this.encodedRaw
	}

	override def sign(privateKeyBytes: ImmutableBytes): Unit = {
		parse.sign(privateKeyBytes)
		this.encodedBytes = null
	}

	override def nonce: BigIntBytes = parse.nonce

	override def senderAddress: Address = parse.senderAddress

	override def value: BigIntBytes = parse.value

	override def receiverAddress: Address = parse.receiverAddress

	override def manaPrice: BigIntBytes = parse.manaPrice

	override def manaLimit: BigIntBytes = parse.manaLimit

	override def data: ImmutableBytes = parse.data

	override def signatureOption: Option[ECDSASignature] = parse.signatureOption

}


object Transaction {
	val logger = LoggerFactory.getLogger("general")

	private val DEFAULT_MANA_PRICE = BigInt("10000000000000")
	private val DEFAULT_BALANCE_MANA = BigInt("21000")

	def decode(src: RBACCodec.Decoder.DecodedResult): TransactionLike = {
		new EncodedTransaction(src.bytes, src.items)
	}

	def decode(rawData: ImmutableBytes): TransactionLike = {
		decode(RBACCodec.Decoder.decode(rawData).right.get)
	}

	def apply(nonce: BigIntBytes, manaPrice: BigIntBytes, manaLimit: BigIntBytes, receiveAddress: Address, value: BigIntBytes, data: ImmutableBytes): TransactionLike = {
		new UnsignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data)
	}

	def apply(nonce: BigIntBytes, manaPrice: BigIntBytes, manaLimit: BigIntBytes, receiveAddress: Address, value: BigIntBytes, data: ImmutableBytes, r: ImmutableBytes, s: ImmutableBytes, v: Byte): TransactionLike = {
		val signature = ECDSASignature(r, s, v)
		new SignedTransaction(nonce, value, receiveAddress, manaPrice, manaLimit, data, signature)
	}

	def create(to: String, amount: BigInt, nonce: BigInt, manaPrice: BigInt, manaLimit: BigInt): TransactionLike = {
		Transaction.apply(BigIntBytes(nonce), BigIntBytes(manaPrice), BigIntBytes(manaLimit), Address160.parseHexString(to), BigIntBytes(amount), ImmutableBytes.empty)
	}

	def createDefault(to: String, amount: BigInteger, nonce: BigInteger): TransactionLike = {
		create(to, amount, nonce, DEFAULT_MANA_PRICE, DEFAULT_BALANCE_MANA)
	}

//	def launderNullToEmpty(bytes: ImmutableBytes): ImmutableBytes = {
//		if (bytes eq null) {
//			ImmutableBytes.empty
//		} else {
//			bytes
//		}
//	}

}
