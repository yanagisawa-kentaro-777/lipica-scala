package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}
import org.spongycastle.math.ec.ECPoint

/**
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class AuthInitiateMessage {
	private var _signature: ECKey.ECDSASignature = null//65 bytes.
	def signature: ECKey.ECDSASignature = this._signature
	def signature_=(v: ECKey.ECDSASignature): Unit = this._signature = v

	private var _ephemeralPublicHash: ImmutableBytes = null//32 bytes.
	def ephemeralPublicHash: ImmutableBytes = this._ephemeralPublicHash
	def ephemeralPublicHash_=(v: ImmutableBytes): Unit = this._ephemeralPublicHash = v

	private var _publicKey: ECPoint = null//64 bytes.
	def publicKey: ECPoint = this._publicKey
	def publicKey_=(v: ECPoint): Unit = this._publicKey = v

	private var _nonce: ImmutableBytes = null//32 bytes.
	def nonce: ImmutableBytes = this._nonce
	def nonce_=(v: ImmutableBytes): Unit = this._nonce = v

	private var _isTokenUsed: Boolean = false//1 byte
	def isTokenUsed: Boolean = this._isTokenUsed
	def isTokenUsed_=(v: Boolean): Unit = this._isTokenUsed = v

	def encode: Array[Byte] = {
		val rsigPad = new Array[Byte](32)
		val rsig = ByteUtils.asUnsignedByteArray(signature.r)
		System.arraycopy(rsig, 0, rsigPad, rsigPad.length - rsig.length, rsig.length)

		val ssigPad = new Array[Byte](32)
		val ssig = ByteUtils.asUnsignedByteArray(signature.s)
		System.arraycopy(ssig, 0, ssigPad, ssigPad.length - ssig.length, ssig.length)

		val sigBytes = rsigPad ++ ssigPad ++ Array[Byte](EncryptionHandshake.recIdFromSignatureV(signature.v))

		val buffer = new Array[Byte](AuthInitiateMessage.length)
		var offset = 0
		System.arraycopy(sigBytes, 0, buffer, offset, sigBytes.length)
		offset += sigBytes.length
		ephemeralPublicHash.copyTo(0, buffer, offset, ephemeralPublicHash.length)
		offset += ephemeralPublicHash.length
		val publicBytes = publicKey.getEncoded(false)
		System.arraycopy(publicBytes, 1, buffer, offset, publicBytes.length - 1)
		offset += publicBytes.length - 1
		nonce.copyTo(0, buffer, offset, nonce.length)
		offset += nonce.length
		buffer(offset) = (if (isTokenUsed) 0x01 else 0x00).toByte
		offset += 1
		buffer
	}

}

object AuthInitiateMessage {
	val length = 65 + 32 + 64 + 32 + 1

	def decode(wire: Array[Byte]): AuthInitiateMessage = {
		val message = new AuthInitiateMessage
		var offset = 0
		val r = new Array[Byte](32)
		val s = new Array[Byte](32)
		System.arraycopy(wire, offset, r, 0, 32)
		offset += 32
		System.arraycopy(wire, offset, s, 0, 32)
		offset += 32
		val v = wire(offset) + 27
		offset += 1
		message.signature = ECKey.ECDSASignature.fromComponents(r, s, v.toByte)
		val ephemeralPublicHash = new Array[Byte](32)
		System.arraycopy(wire, offset, ephemeralPublicHash, 0, 32)
		message.ephemeralPublicHash = ImmutableBytes(ephemeralPublicHash)

		offset += 32
		val bytes = new Array[Byte](65)
		System.arraycopy(wire, offset, bytes, 1, 64)
		offset += 64
		bytes(0) = 0x04
		message.publicKey = ECKey.CURVE.getCurve.decodePoint(bytes)
		val nonce = new Array[Byte](32)
		System.arraycopy(wire, offset, nonce, 0, 32)
		message.nonce = ImmutableBytes(nonce)
		offset += message.nonce.length
		val tokenUsed = wire(offset)
		offset += 1
		if (tokenUsed != 0x00 && tokenUsed != 0x01) throw new RuntimeException("invalid boolean")
		message.isTokenUsed = tokenUsed == 0x01
		message
	}
}
