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
class Transaction {

	//TODO refactor.

	import Transaction._

	/* SHA3 hash of the RBAC encoded transaction */
	private var hash: Array[Byte] = null

	/* a counter used to make sure each transaction can only be processed once */
	private var nonce: Array[Byte] = null

	/* the amount of lipica to transfer (calculated as wei) */
	private var value: Array[Byte] = null

	/**
	 *  the address of the destination account
	 * In creation transaction the receive address is - 0
	 */
	private var receiveAddress: Array[Byte] = null

	/**
	 * the amount of lipica to pay as a transaction fee
	 * to the miner for each unit of mana
	 */
	private var manaPrice: Array[Byte] = null

	/**
	 * The amount of "mana" to allow for the computation.
	 * Mana is the fuel of the computational engine;
	 * every computational step taken and every byte added
	 * to the state or transaction list consumes some mana.
	 * */
	private var manaLimit: Array[Byte] = null

	/**
	 * An unlimited size byte array specifying
	 * input [data] of the message call or
	 * Initialization code for a new contract
	 * */
	private var data: Array[Byte] = null

	/**
	 *  the elliptic curve signature
	 * (including public key recovery bits)
	 * */
	private var signature: ECDSASignature = null

	protected var sendAddress: Array[Byte] = null

	/* Tx in encoded form */
	protected var rlpEncoded: Array[Byte] = null

	private var rlpRaw: Array[Byte] = null

	/**
	 * Indicates if this transaction has been parsed
	 * from the RLP-encoded data
	 * */
	private var parsed = false

	def rlpParse(): Unit = {
		val decodedTxList = RBACCodec.Decoder.decode(rlpEncoded).right.get
		val transaction = decodedTxList.items.head.items
		this.nonce = transaction.head.bytes
		this.manaPrice = transaction(1).bytes
		this.manaLimit = transaction(2).bytes
		this.receiveAddress = transaction(3).bytes
		this.value = transaction(4).bytes
		this.data = transaction(5).bytes
		if (transaction(6).bytes ne null) {
			val v = transaction(6).bytes(0)
			val r = transaction(7).bytes
			val s = transaction(8).bytes
			this.signature = ECDSASignature.fromComponents(r, s, v)
		} else {
			logger.debug("RLP encoded tx is not signed!")
		}
		this.parsed = true
		this.hash = getHash
	}

	/**
	 * このトランザクションにかかるマナの量を返します。
	 */
	def transactionCost: Long = {
		if (!parsed) rlpParse()
		val nonZeroes = nonZeroDataBytes
		val zeroVals = ArrayUtils.getLength(data) - nonZeroes
		ManaCost.TRANSACTION + zeroVals * ManaCost.TX_ZERO_DATA + nonZeroes * ManaCost.TX_NO_ZERO_DATA
	}

	def getHash: Array[Byte] = {
		if (!parsed) rlpParse()
		val plainBytes = this.getEncoded
		DigestUtils.sha3(plainBytes)
	}

	def getRawHash: Array[Byte] = {
		if (!parsed) rlpParse()
		val plainMsg: Array[Byte] = this.getEncodedRaw
		DigestUtils.sha3(plainMsg)
	}

	def getNonce: Array[Byte] = {
		if (!parsed) rlpParse()
		if (nonce == null) zeroByteArray else nonce
	}

	def getValue: Array[Byte] = {
		if (!parsed) rlpParse()
		if (value eq null) zeroByteArray else value
	}

	def getReceiveAddress: Array[Byte] = {
		if (!parsed) rlpParse()
		receiveAddress
	}

	def getGasPrice: Array[Byte] = {
		if (!parsed) rlpParse()
		if (this.manaPrice == null) zeroByteArray else manaPrice
	}

	def getGasLimit: Array[Byte] = {
		if (!parsed) rlpParse()
		manaLimit
	}

	private def nonZeroDataBytes: Int = {
		if (data == null) return 0
		this.data.count(each => each != 0)
	}

	def getData: Array[Byte] = {
		if (!parsed) rlpParse()
		data
	}

	def getSignature: ECKey.ECDSASignature = {
		if (!parsed) rlpParse()
		signature
	}

	def getContractAddress: Array[Byte] = {
		if (!isContractCreation) return null
		DigestUtils.computeNewAddress(this.getSender, this.getNonce)
	}

	def isContractCreation: Boolean = {
		if (!parsed) rlpParse()
		this.receiveAddress == null || (this.receiveAddress sameElements zeroByteArray)
	}

	def getKey: ECKey = {
		val hash = getRawHash
		ECKey.recoverFromSignature(signature.v, signature, hash, true)
	}

	def getSender: Array[Byte] = {
		try {
			if (sendAddress eq null) {
				val key = ECKey.signatureToKey(getRawHash, getSignature.toBase64)
				sendAddress = key.getAddress
			}
			sendAddress
		} catch {
			case e: SignatureException =>
				logger.error(e.getMessage, e)
				null
		}
	}

	def sign(privateKeyBytes: Array[Byte]): Unit = {
		val hash = this.getRawHash
		val key = ECKey.fromPrivate(privateKeyBytes).decompress
		this.signature = key.sign(hash)
		this.rlpEncoded = null
	}

	def getEncodedRaw: Array[Byte] = {
		if (!parsed) rlpParse()
		if (this.rlpRaw ne null) return this.rlpRaw
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

		this.rlpRaw = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, value, data))
		this.rlpRaw
	}

	def getEncoded: Array[Byte] = {
		if (this.rlpEncoded ne null) return this.rlpEncoded
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


		val (v, r, s) =
			if (signature != null) {
				val v = RBACCodec.Encoder.encode(signature.v)
				val r = RBACCodec.Encoder.encode(ByteUtils.asUnsignedByteArray(signature.r))
				val s = RBACCodec.Encoder.encode(ByteUtils.asUnsignedByteArray(signature.s))
				(v, r, s)
			} else {
				val v = RBACCodec.Encoder.encode(Array.emptyByteArray)
				val r = RBACCodec.Encoder.encode(Array.emptyByteArray)
				val s = RBACCodec.Encoder.encode(Array.emptyByteArray)
				(v, r, s)
			}

		this.rlpEncoded = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, manaPrice, manaLimit, receiveAddress, value, data, v, r, s))
		this.hash = getHash
		this.rlpEncoded
	}

	override def hashCode: Int = {
		java.util.Arrays.hashCode(this.hash)
	}

	override def equals(o: Any): Boolean = {
		try {
			o.asInstanceOf[Transaction].hash sameElements this.hash
		} catch {
			case any: Throwable => false
		}
	}
}

object Transaction {

	private val logger = LoggerFactory.getLogger(getClass)
	private val DEFAULT_MANA_PRICE = BigInt("10000000000000")
	private val DEFAULT_BALANCE_MANA = BigInt("21000")

	val zeroByteArray = Array[Byte](0)

	def apply(rawData: Array[Byte]): Transaction = {
		val result = new Transaction
		result.rlpEncoded = rawData
		result.parsed = false
		result
	}

	def apply(nonce: Array[Byte], manaPrice: Array[Byte], manaLimit: Array[Byte], receiveAddress: Array[Byte], value: Array[Byte], data: Array[Byte]): Transaction = {
		val result = new Transaction
		result.nonce = nonce
		result.manaPrice = manaPrice
		result.manaLimit = manaLimit
		result.receiveAddress = receiveAddress
		if (result.receiveAddress eq null) {
			result.receiveAddress = Array.emptyByteArray
		}
		result.value = value
		result.data = data
		result.parsed = true
		result
	}

	def apply(nonce: Array[Byte], manaPrice: Array[Byte], manaLimit: Array[Byte], receiveAddress: Array[Byte], value: Array[Byte], data: Array[Byte], r: Array[Byte], s: Array[Byte], v: Byte): Transaction = {
		val result = apply(nonce, manaPrice, manaLimit, receiveAddress, value, data)
		val signature: ECKey.ECDSASignature = new ECKey.ECDSASignature(new BigInteger(r), new BigInteger(s))
		signature.v = v
		result.signature = signature
		result
	}

	def create(to: String, amount: BigInt, nonce: BigInt, manaPrice: BigInt, manaLimit: BigInt): Transaction = {
		Transaction.apply(ByteUtils.asUnsignedByteArray(nonce), ByteUtils.asUnsignedByteArray(manaPrice), ByteUtils.asUnsignedByteArray(manaLimit), Hex.decodeHex(to.toCharArray), ByteUtils.asUnsignedByteArray(amount), null)
	}

	def createDefault(to: String, amount: BigInteger, nonce: BigInteger): Transaction = {
		create(to, amount, nonce, DEFAULT_MANA_PRICE, DEFAULT_BALANCE_MANA)
	}


}