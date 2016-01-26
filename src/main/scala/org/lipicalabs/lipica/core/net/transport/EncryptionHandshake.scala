package org.lipicalabs.lipica.core.net.transport

import java.security.SecureRandom

import org.lipicalabs.lipica.core.crypto.{ECIESCoder, ECKey}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.math.ec.ECPoint

/**
 * 通信経路を暗号化するための
 * 共通鍵を合意する機構を実装したクラスです。
 *
 * アルゴリズムとしては、ECDHE（Elliptic Curve Diffie–Hellman key agreement by Ephemeral keys）を使用しています。
 *
 * @param isInitiator セッション開始要求側（＝クライアント側）であるか否か。
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class EncryptionHandshake private(val isInitiator: Boolean) {
	import EncryptionHandshake._

	private val random = new SecureRandom

	/**
	 * 自ノードの一時鍵。
	 */
	private val ephemeralKey: ECKey = new ECKey(this.random)

	/**
	 * 対向ノードの公開鍵。
	 */
	private var _remotePublicKey: ECPoint = null
	def remotePublicKey: ECPoint = this._remotePublicKey

	/**
	 * 対向ノードの一時鍵の公開鍵。
	 */
	private var _remoteEphemeralKey: ECPoint = null
	def remoteEphemeralKey: ECPoint = this._remoteEphemeralKey

	/**
	 * 開始要求側（＝クライアント側）のランダム要素。
	 */
	private var _initiatorNonce: ImmutableBytes = null
	def initiatorNonce: ImmutableBytes = this._initiatorNonce

	/**
	 * 応答側（＝サーバー側）のランダム要素。
	 */
	private var _responderNonce: ImmutableBytes = null
	def responderNonce: ImmutableBytes = this._responderNonce

	/**
	 * 秘密情報。
	 */
	private var _secrets: Secrets = null
	def secrets: Secrets = this._secrets

	/**
	 * 渡された鍵ペアを自ノードの鍵ペアとして、
	 * セッション確立要求メッセージを作成します。
	 */
	def createAuthInitiate(key: ECKey): AuthInitiateMessage = createAuthInitiate(null, key)

	/**
	 * 渡された鍵ペアを自ノードの鍵ペアとして、
	 * セッション確立要求メッセージを作成します。
	 */
	def createAuthInitiate(aToken: Array[Byte], myKey: ECKey): AuthInitiateMessage = {
		val (isToken, token) =
			if (aToken eq null) {
				val secretScalar = this.remotePublicKey.multiply(myKey.getPrivKey).normalize.getXCoord.toBigInteger
				(false, ByteUtils.bigIntegerToBytes(secretScalar, NonceSize))
			} else {
				(true, aToken)
			}
		val nonce = this.initiatorNonce
		val signed = xor(token, nonce.toByteArray)
		val signature = this.ephemeralKey.sign(signed)
		val isTokenUsed = isToken
		val dataSlice = java.util.Arrays.copyOfRange(this.ephemeralKey.getPubKeyPoint.getEncoded(false), 1, 1 + 64)
		//一時キーのハッシュ値。
		val ephemeralPublicHash = ImmutableBytes(DigestUtils.digest256(dataSlice))
		//自ノードの公開鍵。
		val publicKey = myKey.getPubKeyPoint

		new AuthInitiateMessage(signature, ephemeralPublicHash, publicKey, nonce, isTokenUsed)
	}

	/**
	 * 渡されたセッション確立要求メッセージを、暗号化されたバイト列に変換して返します。
	 */
	def encryptAuthInitiate(message: AuthInitiateMessage): Array[Byte] = {
		ECIESCoder.encrypt(this.remotePublicKey, message.encode)
	}

	/**
	 * 渡された暗号化バイト列を、セッション確立要求メッセージに復号して返します。
	 */
	def decryptAuthInitiate(encryptedBytes: Array[Byte], myKey: ECKey): AuthInitiateMessage = {
		val plainBytes = ECIESCoder.decrypt(myKey.getPrivKey, encryptedBytes)
		AuthInitiateMessage.decode(plainBytes)
	}

	/**
	 * 渡された応答メッセージを、暗号化されたバイト列に変換して返します。
	 */
	def encryptAuthResponse(message: AuthResponseMessage): Array[Byte] = {
		ECIESCoder.encrypt(this.remotePublicKey, message.encode)
	}

	/**
	 * 渡された暗号化バイト列を、応答メッセージに復号して返します。
	 */
	def decryptAuthResponse(encryptedBytes: Array[Byte], myKey: ECKey): AuthResponseMessage = {
		val plainBytes = ECIESCoder.decrypt(myKey.getPrivKey, encryptedBytes)
		AuthResponseMessage.decode(plainBytes)
	}

	/**
	 * 対向ノードから受信したセッション確立要求を解釈して
	 * 秘密情報を合意し、応答メッセージを生成して返します。
	 */
	def handleAuthInitiate(initiatePacket: Array[Byte], key: ECKey): Array[Byte] = {
		val response = makeResponse(initiatePacket, key)
		val responsePacket = encryptAuthResponse(response)
		this._secrets = agreeSecrets(initiatePacket, responsePacket)
		responsePacket
	}

	/**
	 * 対向ノードから受信した応答メッセージを解釈して
	 * 秘密情報を合意し、応答メッセージを生成して返します。
	 */
	def handleAuthResponse(myKey: ECKey, initiatePacket: Array[Byte], responsePacket: Array[Byte]): AuthResponseMessage = {
		val response = decryptAuthResponse(responsePacket, myKey)
		this._remoteEphemeralKey = response.ephemeralPublicKey
		this._responderNonce = response.nonce
		this._secrets = agreeSecrets(initiatePacket, responsePacket)
		response
	}

	/**
	 * 渡されたセッション確立要求に対する応答メッセージを生成して返します。
	 */
	private def makeResponse(initiatePacket: Array[Byte], key: ECKey): AuthResponseMessage = {
		val initiate = decryptAuthInitiate(initiatePacket, key)
		makeResponse(initiate, key)
	}

	/**
	 * 渡されたセッション確立要求に対する応答メッセージを生成して返します。
	 */
	def makeResponse(initiateMessage: AuthInitiateMessage, key: ECKey): AuthResponseMessage = {
		this._initiatorNonce = initiateMessage.nonce
		this._remotePublicKey = initiateMessage.publicKey

		val secretScalar = this.remotePublicKey.multiply(key.getPrivKey).normalize().getXCoord.toBigInteger
		val token = ByteUtils.bigIntegerToBytes(secretScalar, NonceSize)
		val signed = xor(token, this.initiatorNonce.toByteArray)

		val ephemeral = ECKey.recoverFromSignature(recIdFromSignatureV(initiateMessage.signature.v), initiateMessage.signature, signed, false)
		this._remoteEphemeralKey = ephemeral.getPubKeyPoint

		new AuthResponseMessage(this.ephemeralKey.getPubKeyPoint, this.responderNonce, initiateMessage.isTokenUsed)
	}

	/**
	 * セッション確立要求メッセージとそれに対する応答メッセージの組に基いて、
	 * 暗号化のための共通鍵を合意します。
	 */
	def agreeSecrets(initiatePacket: Array[Byte], responsePacket: Array[Byte]): Secrets = {
		val secretScalar = this.remoteEphemeralKey.multiply(ephemeralKey.getPrivKey).normalize.getXCoord.toBigInteger
		val agreedSecret = ByteUtils.bigIntegerToBytes(secretScalar, SecretSize)
		val data1 = (responderNonce ++ initiatorNonce).toByteArray
		val data2 = agreedSecret ++ DigestUtils.digest256(data1)
		val sharedSecret = DigestUtils.digest256(data2)
		val data3 = agreedSecret ++ sharedSecret
		val aesSecret = DigestUtils.digest256(data3)

		val aes = ImmutableBytes(aesSecret)
		val mac = ImmutableBytes(DigestUtils.digest256(agreedSecret ++ aesSecret))
		val token = ImmutableBytes(DigestUtils.digest256(sharedSecret))

		val mac1 = new KeccakDigest(MacSize)
		mac1.update(xor(mac.toByteArray, responderNonce.toByteArray), 0, mac.length)
		val buf = new Array[Byte](32)
		new KeccakDigest(mac1).doFinal(buf, 0)
		mac1.update(initiatePacket, 0, initiatePacket.length)
		new KeccakDigest(mac1).doFinal(buf, 0)
		val mac2 = new KeccakDigest(MacSize)
		mac2.update(xor(mac.toByteArray, initiatorNonce.toByteArray), 0, mac.length)
		new KeccakDigest(mac2).doFinal(buf, 0)
		mac2.update(responsePacket, 0, responsePacket.length)
		new KeccakDigest(mac2).doFinal(buf, 0)
		if (isInitiator) {
			new Secrets(aes = aes, mac = mac, token = token, egressMac = mac1, ingressMac = mac2)
		} else {
			new Secrets(aes = aes, mac = mac, token = token, egressMac = mac2, ingressMac = mac1)
		}
	}

}

object EncryptionHandshake {
	
	/**
	 * ECDHE によって合意される秘密情報を表現したクラスです。
	 *
	 * @param aes AES暗号化に使用される共通鍵。
	 * @param mac 送受信フレームごとの message authentication code を計算する際に利用するシード情報。
	 * @param token 外部から指定されたトークンがあるならばそれ。
	 * @param egressMac 自ノードからの出力（＝送信データ）によって更新され続けるダイジェスト計算器。
	 * @param ingressMac 自ノードへの入力（＝受信データ）によって更新され続けるダイジェスト計算器。
	 */
	class Secrets(val aes: ImmutableBytes, val mac: ImmutableBytes, val token: ImmutableBytes, val egressMac: KeccakDigest, val ingressMac: KeccakDigest) {
		//
	}

	val NonceSize = 32
	val MacSize = 256
	val SecretSize = 32

	/**
	 * 渡された公開鍵を持つノードに
	 * セッション確立を要求する（クライアントとして）モジュールを、
	 * 生成して返します。
	 */
	def createInitiator(remotePublicKey: ECPoint): EncryptionHandshake = {
		val result = new EncryptionHandshake(isInitiator = true)
		result._remotePublicKey = remotePublicKey
		result._initiatorNonce = ImmutableBytes.createRandom(result.random, NonceSize)
		result
	}

	/**
	 * セッション確立要求への応答側（＝サーバー側）の
	 * モジュールを生成して返します。
	 */
	def createResponder: EncryptionHandshake = {
		val result = new EncryptionHandshake(isInitiator = false)
		//一時鍵。
		result._responderNonce = ImmutableBytes.createRandom(result.random, NonceSize)
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
