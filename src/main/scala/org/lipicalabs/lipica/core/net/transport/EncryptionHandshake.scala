package org.lipicalabs.lipica.core.net.transport

import java.security.SecureRandom

import org.lipicalabs.lipica.core.crypto.{ECIESCoder, ECKey}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.math.ec.ECPoint

/**
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class EncryptionHandshake private() {
	import EncryptionHandshake._

	private val random = new SecureRandom

	private var _isInitiator: Boolean = false
	def isInitiator: Boolean = this._isInitiator
	def isInitiator_=(v: Boolean): Unit = this._isInitiator = v

	private var _ephemeralKey: ECKey = null
	def ephemeralKey: ECKey = this._ephemeralKey
	def ephemeralKey_=(v: ECKey): Unit = this._ephemeralKey = v

	private var _remotePublicKey: ECPoint = null
	def remotePublicKey: ECPoint = this._remotePublicKey
	def remotePublicKey_=(v: ECPoint): Unit = this._remotePublicKey = v

	private var _remoteEphemeralKey: ECPoint = null
	def remoteEphemeralKey: ECPoint = this._remoteEphemeralKey
	def remoteEphemeralKey_=(v: ECPoint): Unit = this._remoteEphemeralKey = v

	private var _initiatorNonce: ImmutableBytes = null
	def initiatorNonce: ImmutableBytes = this._initiatorNonce
	def initiatorNonce_=(v: ImmutableBytes): Unit = this._initiatorNonce = v

	private var _responderNonce: ImmutableBytes = null
	def responderNonce: ImmutableBytes = this._responderNonce
	def responderNonce_=(v: ImmutableBytes): Unit = this._responderNonce = v

	private var _secrets: Secrets = null
	def secrets: Secrets = this._secrets
	def secrets_=(v: Secrets): Unit = this._secrets = v

	def createAuthInitiate(key: ECKey): AuthInitiateMessage = createAuthInitiate(null, key)

	def createAuthInitiate(aToken: Array[Byte], key: ECKey): AuthInitiateMessage = {
		val message = new AuthInitiateMessage
		val (isToken, token) =
			if (aToken eq null) {
				val secretScalar = this.remotePublicKey.multiply(key.getPrivKey).normalize.getXCoord.toBigInteger
				(false, ByteUtils.bigIntegerToBytes(secretScalar, NonceSize))
			} else {
				(true, aToken)
			}
		val nonce = this.initiatorNonce
		val signed = xor(token, nonce.toByteArray)
		message.signature = this.ephemeralKey.sign(signed)
		message.isTokenUsed = isToken
		val dataSlice = java.util.Arrays.copyOfRange(this.ephemeralKey.getPubKeyPoint.getEncoded(false), 1, 1 + 64)
		message.ephemeralPublicHash = ImmutableBytes(DigestUtils.digest256(dataSlice))
		message.publicKey = key.getPubKeyPoint
		message.nonce = this.initiatorNonce
		message
	}

	def encryptAuthInitiate(message: AuthInitiateMessage): Array[Byte] = {
		ECIESCoder.encrypt(this.remotePublicKey, message.encode)
	}

	def encryptAuthResponse(message: AuthResponseMessage): Array[Byte] = {
		ECIESCoder.encrypt(this.remotePublicKey, message.encode)
	}

	def decryptAuthInitiate(encryptedBytes: Array[Byte], myKey: ECKey): AuthInitiateMessage = {
		val plainBytes = ECIESCoder.decrypt(myKey.getPrivKey, encryptedBytes)
		AuthInitiateMessage.decode(plainBytes)
	}

	def decryptAuthResponse(encryptedBytes: Array[Byte], myKey: ECKey): AuthResponseMessage = {
		val plainBytes = ECIESCoder.decrypt(myKey.getPrivKey, encryptedBytes)
		AuthResponseMessage.decode(plainBytes)
	}

	def handleAuthInitiate(initiatePacket: Array[Byte], key: ECKey): Array[Byte] = {
		val response = makeResponse(initiatePacket, key)
		val responsePacket = encryptAuthResponse(response)
		agreeSecret(initiatePacket, responsePacket)
		responsePacket
	}

	def handleAuthResponse(myKey: ECKey, initiatePacket: Array[Byte], responsePacket: Array[Byte]): AuthResponseMessage = {
		val response = decryptAuthResponse(responsePacket, myKey)
		this.remoteEphemeralKey = response.ephemeralPublicKey
		this.responderNonce = response.nonce
		agreeSecret(initiatePacket, responsePacket)
		response
	}

	def makeResponse(initiatePacket: Array[Byte], key: ECKey): AuthResponseMessage = {
		val initiate = decryptAuthInitiate(initiatePacket, key)
		makeResponse(initiate, key)
	}

	def makeResponse(initiate: AuthInitiateMessage, key: ECKey): AuthResponseMessage = {
		this.initiatorNonce = initiate.nonce
		this.remotePublicKey = initiate.publicKey

		val secretScalar = this.remotePublicKey.multiply(key.getPrivKey).normalize().getXCoord.toBigInteger
		val token = ByteUtils.bigIntegerToBytes(secretScalar, NonceSize)
		val signed = xor(token, this.initiatorNonce.toByteArray)

		val ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiate.signature.v), initiate.signature, signed, false)
		this.remoteEphemeralKey = ephemeral.getPubKeyPoint
		val response = new AuthResponseMessage
		response.isTokenUsed = initiate.isTokenUsed
		response.ephemeralPublicKey = this.ephemeralKey.getPubKeyPoint
		response.nonce = this.responderNonce
		response
	}

	def agreeSecret(initiatePacket: Array[Byte], responsePacket: Array[Byte]): Unit = {
		val secretScalar = this.remoteEphemeralKey.multiply(ephemeralKey.getPrivKey).normalize.getXCoord.toBigInteger
		val agreedSecret = ByteUtils.bigIntegerToBytes(secretScalar, SecretSize)
		val data1 = (responderNonce ++ initiatorNonce).toByteArray
		val data2 = agreedSecret ++ DigestUtils.digest256(data1)
		val sharedSecret = DigestUtils.digest256(data2)
		val data3 = agreedSecret ++ sharedSecret
		val aesSecret = DigestUtils.digest256(data3)
		this.secrets = new EncryptionHandshake.Secrets
		secrets.aes = ImmutableBytes(aesSecret)
		secrets.mac = ImmutableBytes(DigestUtils.digest256(agreedSecret ++ aesSecret))
		secrets.token = ImmutableBytes(DigestUtils.digest256(sharedSecret))
		//        System.out.println("mac " + Hex.toHexString(secrets.mac));
		//        System.out.println("aes " + Hex.toHexString(secrets.aes));
		//        System.out.println("shared " + Hex.toHexString(sharedSecret));
		//        System.out.println("ecdhe " + Hex.toHexString(agreedSecret));
		val mac1 = new KeccakDigest(MacSize)
		mac1.update(xor(secrets.mac.toByteArray, responderNonce.toByteArray), 0, secrets.mac.length)
		val buf = new Array[Byte](32)
		new KeccakDigest(mac1).doFinal(buf, 0)
		mac1.update(initiatePacket, 0, initiatePacket.length)
		new KeccakDigest(mac1).doFinal(buf, 0)
		val mac2 = new KeccakDigest(MacSize)
		mac2.update(xor(secrets.mac.toByteArray, initiatorNonce.toByteArray), 0, secrets.mac.length)
		new KeccakDigest(mac2).doFinal(buf, 0)
		mac2.update(responsePacket, 0, responsePacket.length)
		new KeccakDigest(mac2).doFinal(buf, 0)
		if (isInitiator) {
			secrets.egressMac = mac1
			secrets.ingressMac = mac2
		}
		else {
			secrets.egressMac = mac2
			secrets.ingressMac = mac1
		}
	}

}

object EncryptionHandshake {

	class Secrets {
		var aes: ImmutableBytes = null
		var mac: ImmutableBytes = null
		var token: ImmutableBytes = null
		var egressMac: KeccakDigest = null
		var ingressMac: KeccakDigest = null
	}

	val NonceSize = 32
	val MacSize = 256
	val SecretSize = 32

	def createInitiator(remotePublicKey: ECPoint): EncryptionHandshake = {
		val result = new EncryptionHandshake
		result.remotePublicKey = remotePublicKey
		result.ephemeralKey = new ECKey(result.random)
		result.initiatorNonce = ImmutableBytes.createRandom(result.random, NonceSize)
		result.isInitiator = true
		result
	}

	def createResponder: EncryptionHandshake = {
		val result = new EncryptionHandshake
		result.ephemeralKey = new ECKey(result.random)
		result.responderNonce = ImmutableBytes.createRandom(result.random, NonceSize)
		result.isInitiator = false
		result
	}

	def recIdFromSignatureV(v: Int): Byte = {
		val value =
			if (31 <= v) {
				v - 4
			} else {
				v
			}
		(value - 27).toByte
	}

	private def xor(b1: Array[Byte], b2: Array[Byte]): Array[Byte] = {
		val result = new Array[Byte](b1.length)
		for (i <- b1.indices) {
			result(i) = (b1(i) ^ b2(i)).toByte
		}
		result
	}
	
}
