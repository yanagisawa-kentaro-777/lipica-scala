package org.lipicalabs.lipica.core.crypto.elliptic_curve

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.kernel.{Address160, Address}
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts, ByteUtils}
import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.asn1.x9.{X9IntegerConverter, X9ECParameters}
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.params.{ECPublicKeyParameters, ECPrivateKeyParameters, ECKeyGenerationParameters, ECDomainParameters}
import org.spongycastle.crypto.signers.{HMacDSAKCalculator, ECDSASigner}
import org.spongycastle.math.ec.{ECCurve, ECAlgorithms, ECPoint}
import org.spongycastle.util.encoders.Base64

/**
 * 非対称鍵暗号の一種である楕円曲線暗号の
 * 「鍵ペア」もしくは「公開鍵のみ」をモデル化した trait です。
 *
 * @since 2016/01/24
 * @author YANAGISAWA, Kentaro
 */
trait ECKeyLike {

	def publicKeyPoint: ECPoint

	def isCompressed: Boolean

	def decompress: ECKeyLike

	/**
	 * このオブジェクトの公開鍵に基づいて、アドレスを生成して返します。
	 */
	def toAddress: Address = {
		val publicKeyBytes = this.publicKeyPoint.getEncoded(false)
		val pubKeyHash = DigestUtils.digest256omit12Bytes(java.util.Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length))
		Address160(pubKeyHash)
	}

	/**
	 * このオブジェクトの公開鍵に基づいて、ノードIDを生成して返します。
	 */
	def toNodeId: NodeId = {
		val nodeIdWithFormat = this.publicKeyPoint.getEncoded(false)
		val result = new Array[Byte](NodeId.NumBytes)
		System.arraycopy(nodeIdWithFormat, 1, result, 0, result.length)
		NodeId(result)
	}

	override def hashCode: Int = {
		java.util.Arrays.hashCode(this.publicKeyPoint.getEncoded(false))
	}

	override def toString: String = {
		Hex.encodeHexString(this.publicKeyPoint.getEncoded(false))
	}

}

object ECKeyLike {
	/**
	 * secp256k1 を利用することが前提である。
	 */
	private val params: X9ECParameters = SECNamedCurves.getByName("secp256k1")
	val CURVE = new ECDomainParameters(params.getCurve, params.getG, params.getN, params.getH)
	val HALF_CURVE_ORDER = BigInt(params.getN.shiftRight(1))

	private[elliptic_curve] def compressPoint(uncompressed : ECPoint): ECPoint = {
		CURVE.getCurve.decodePoint(uncompressed.getEncoded(true))
	}

	private[elliptic_curve] def decompressKey (xBN : BigInt, yBit : Boolean): ECPoint = {
		val x9 = new X9IntegerConverter
		val compEnc = x9.integerToBytes(xBN.bigInteger, 1 + x9.getByteLength(CURVE.getCurve))
		compEnc(0) = (if (yBit) 0x03 else 0x02).toByte
		CURVE.getCurve.decodePoint(compEnc)
	}

}


/**
 * 非対称鍵暗号の一種である楕円曲線暗号の
 * 「公開鍵」をモデル化したクラスです。
 *
 * @since 2016/01/24
 * @author YANAGISAWA, Kentaro
 */

class ECPublicKey private[elliptic_curve](override val publicKeyPoint: ECPoint, override val isCompressed: Boolean) extends ECKeyLike {
	import ECKeyLike._

	override def decompress: ECPublicKey = new ECPublicKey(CURVE.getCurve.decodePoint(this.publicKeyPoint.getEncoded(false)), isCompressed = false)

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[ECPublicKey]
			(this.publicKeyPoint == another.publicKeyPoint) && (this.isCompressed == another.isCompressed)
		} catch {
			case e: Throwable => false
		}
	}
}

object ECPublicKey {
	import ECKeyLike._
	/**
	 * 渡された秘密鍵から、公開鍵オブジェクトを生成して返します。
	 * 公開鍵オブジェクトは署名の検証をすることはできますが、
	 * 署名を付与することはできません。
	 */
	def fromPublicOnly(publicKey: Array[Byte]): ECPublicKey = {
		new ECPublicKey(compressPoint(CURVE.getCurve.decodePoint(publicKey)), isCompressed = true)
	}

	/**
	 * 渡された署名から、公開鍵を復元して返します。
	 */
	def recoverFromSignature(recId: Int, sig: ECDSASignature, messageHash: Array[Byte], compressed: Boolean): Option[ECPublicKey] = {
		val n = BigInt(CURVE.getN)
		val i = BigInt(recId.toLong / 2)
		val x = sig.r + (i * n)
		//   1.2. Convert the integer x to an octet string X of length mlen using the conversion routine
		//        specified in Section 2.3.7, where mlen = ?(log2 p)/8? or mlen = ?m/8?.
		//   1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R using the
		//        conversion routine specified in Section 2.3.4. If this conversion routine outputs "invalid" , then
		//        do another iteration of Step 1.
		//
		// More concisely, what these points mean is to use X as a compressed public key.
		val curve = CURVE.getCurve.asInstanceOf[ECCurve.Fp]
		val prime = BigInt(curve.getQ)
		if (prime <= x) {
			return None
		}
		// Compressed keys require you to know an extra bit of data about the y-coord as there are two possibilities.
		// So it's encoded in the recId.
		val R = decompressKey(x, (recId & 1) == 1)
		//   1.4. If nR != point at infinity, then do another iteration of Step 1 (callers responsibility).
		if (!R.multiply(n.bigInteger).isInfinity) {
			return None
		}
		//   1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
		val e = BigInt(1, messageHash)
		//   1.6. For k from 1 to 2 do the following.   (loop is outside this function via iterating recId)
		//   1.6.1. Compute a candidate public key as:
		//               Q = mi(r) * (sR - eG)
		//
		// Where mi(x) is the modular multiplicative inverse. We transform this into the following:
		//               Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
		// Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n). In the above equation
		// ** is point multiplication and + is point addition (the EC group operator).
		//
		// We can find the additive inverse by subtracting e from zero then taking the mod. For example the additive
		// inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 = 8.
		val eInv = (UtilConsts.Zero - e) % n
		val rInv = sig.r.modInverse(n)
		val srInv = (rInv * sig.s) % n
		val eInvrInv = (rInv * eInv) % n
		val q = ECAlgorithms.sumOfTwoMultiplies(CURVE.getG, eInvrInv.bigInteger, R, srInv.bigInteger).asInstanceOf[ECPoint.Fp]
		Option(ECPublicKey.fromPublicOnly(q.getEncoded(compressed)))
	}

	/**
	 * 渡された署名から、公開鍵を復元して返します。
	 */
	def recoverFromSignature(messageHash: Array[Byte], signatureBase64: String): Option[ECPublicKey] = {
		val signatureEncoded = Base64.decode(signatureBase64)
		if (signatureEncoded.length < 65) {
			return None
		}
		var header: Int = signatureEncoded(0) & 0xFF
		// The header byte: 0x1B = first key with even y, 0x1C = first key with odd y,
		//                  0x1D = second key with even y, 0x1E = second key with odd y
		if (header < 27 || header > 34) {
			return None
		}
		val r = BigInt(1, java.util.Arrays.copyOfRange(signatureEncoded, 1, 33))
		val s = BigInt(1, java.util.Arrays.copyOfRange(signatureEncoded, 33, 65))
		val sig = new ECDSASignature(r, s)
		var compressed = false
		if (header >= 31) {
			compressed = true
			header -= 4
		}
		val recId = header - 27
		recoverFromSignature(recId, sig, messageHash, compressed)
	}

}

/**
 * 非対称鍵暗号の一種である楕円曲線暗号の
 * 「鍵ペア」をモデル化したクラスです。
 * 署名を付与することができます。
 *
 * @since 2016/01/24
 * @author YANAGISAWA, Kentaro
 */
class ECKeyPair private[elliptic_curve](override val publicKeyPoint: ECPoint, val privateKey: BigInt, override val isCompressed: Boolean) extends ECKeyLike {
	import ECKeyLike._

	override def decompress: ECKeyPair = {
		new ECKeyPair(CURVE.getCurve.decodePoint(this.publicKeyPoint.getEncoded(false)), this.privateKey, isCompressed = false)
	}

	def sign(input: Array[Byte]): ECDSASignature = {
		val sig = privateSign(input)
		(0 until 4).foreach {
			i => {
				val publicKeyOption = ECPublicKey.recoverFromSignature(i, sig, input, isCompressed)
				if (publicKeyOption.exists(_.publicKeyPoint == this.publicKeyPoint)) {
					sig.v = (i + 27 + (if (this.isCompressed) 4 else 0)).toByte
					return sig
				}
			}
		}
		throw new RuntimeException("Failed to construct a recoverable key.")
	}

	private def privateSign(input: Array[Byte]): ECDSASignature = {
		val signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest))
		val privKey = new ECPrivateKeyParameters(this.privateKey.bigInteger, CURVE)
		signer.init(true, privKey)
		val components: Array[BigInteger] = signer.generateSignature(input)
		new ECDSASignature(components(0), components(1)).toCanonicalized
	}

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[ECKeyPair]
			(this.publicKeyPoint == another.publicKeyPoint) &&
				(this.privateKey == another.privateKey) &&
				(this.isCompressed == another.isCompressed)
		} catch {
			case e: Throwable => false
		}
	}

}

object ECKeyPair {
	import ECKeyLike._

	/**
	 * 渡された擬似乱数生成器を使って、新たなEC鍵ペアを生成して返します。
	 */
	def apply(random: SecureRandom): ECKeyPair = {
		val generator = new ECKeyPairGenerator
		val keygenParams = new ECKeyGenerationParameters(ECKeyLike.CURVE, random)
		generator.init(keygenParams)
		val keypair = generator.generateKeyPair
		val privParams = keypair.getPrivate.asInstanceOf[ECPrivateKeyParameters]
		val pubParams = keypair.getPublic.asInstanceOf[ECPublicKeyParameters]
		val privateKey = privParams.getD
		val publicKey = ECKeyLike.CURVE.getCurve.decodePoint(pubParams.getQ.getEncoded(true))
		new ECKeyPair(publicKey, privateKey, isCompressed = true)
	}

	/**
	 * 渡された秘密鍵から、鍵ペアを生成して返します。
	 */
	def fromPrivateKey(privateKey: BigInt): ECKeyPair = {
		new ECKeyPair(compressPoint(CURVE.getG.multiply(privateKey.bigInteger)), privateKey, isCompressed = true)
	}

	/**
	 * 渡された秘密鍵から、鍵ペアを生成して返します。
	 */
	def fromPrivateKey(privateKey: Array[Byte]): ECKeyPair = fromPrivateKey(BigInt(1, privateKey))

}

/**
 * 楕円曲線暗号における署名を表現するクラスです。
 */
class ECDSASignature private[elliptic_curve](val r: BigInt, val s: BigInt) {

	private val vRef: AtomicReference[Byte] = new AtomicReference[Byte](0)
	def v: Byte = this.vRef.get
	def v_=(value: Byte): Unit = this.vRef.set(value)

	def toCanonicalized: ECDSASignature = {
		if (ECKeyLike.HALF_CURVE_ORDER < s) {
			new ECDSASignature(r, BigInt(ECKeyLike.CURVE.getN) - s)
		} else {
			this
		}
	}

	def toBase64: String = {
		val sigData = new Array[Byte](65)
		sigData(0) = v
		System.arraycopy(ByteUtils.bigIntegerToBytes(this.r.bigInteger, 32), 0, sigData, 1, 32)
		System.arraycopy(ByteUtils.bigIntegerToBytes(this.s.bigInteger, 32), 0, sigData, 33, 32)
		new String(Base64.encode(sigData), StandardCharsets.UTF_8)
	}

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[ECDSASignature]
			this.r == another.r && this.s == another.s
		} catch {
			case any: Throwable => false
		}
	}

	override def hashCode: Int = {
		(r.hashCode * 31) + s.hashCode
	}

}

object ECDSASignature {

	def apply(r: Array[Byte], s: Array[Byte], v: Byte): ECDSASignature = {
		val result = new ECDSASignature(BigInt(1, r), BigInt(1, s))
		result.v = v
		result
	}

	def apply(r: ImmutableBytes, s: ImmutableBytes, v: Byte): ECDSASignature = {
		val result = new ECDSASignature(r.toPositiveBigInt, s.toPositiveBigInt)
		result.v = v
		result
	}
}