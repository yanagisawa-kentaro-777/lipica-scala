package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.elliptic_curve.{ECKeyLike, ECDSASignature}
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}
import org.spongycastle.math.ec.ECPoint

/**
 * セッション確立を要求するメッセージです。
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class AuthInitiateMessage(val signature: ECDSASignature, val ephemeralPublicHash: ImmutableBytes, val publicKey: ECPoint, val nonce: ImmutableBytes, val isTokenUsed: Boolean) {

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

	override def toString: String = "AuthInitiateMessage"

}

object AuthInitiateMessage {
	val length = 65 + 32 + 64 + 32 + 1

	def decode(wire: Array[Byte]): AuthInitiateMessage = {
		var offset = 0
		val r = new Array[Byte](32)
		val s = new Array[Byte](32)
		System.arraycopy(wire, offset, r, 0, 32)
		offset += 32
		System.arraycopy(wire, offset, s, 0, 32)
		offset += 32
		val v = wire(offset) + 27
		offset += 1
		val signature = ECDSASignature(r, s, v.toByte)
		val ephemeralPublicHash = new Array[Byte](32)
		System.arraycopy(wire, offset, ephemeralPublicHash, 0, 32)
		val immutableEphemeralPublicHash = ImmutableBytes(ephemeralPublicHash)

		offset += 32
		val bytes = new Array[Byte](65)
		System.arraycopy(wire, offset, bytes, 1, 64)
		offset += 64
		bytes(0) = 0x04
		val publicKey = ECKeyLike.CURVE.getCurve.decodePoint(bytes)
		val nonce = new Array[Byte](32)
		System.arraycopy(wire, offset, nonce, 0, 32)
		val immutableNonce = ImmutableBytes(nonce)
		offset += immutableNonce.length
		val tokenUsed = wire(offset)
		offset += 1
		if (tokenUsed != 0x00 && tokenUsed != 0x01) throw new RuntimeException("invalid boolean")
		val isTokenUsed = tokenUsed == 0x01

		new AuthInitiateMessage(signature, immutableEphemeralPublicHash, publicKey, immutableNonce, isTokenUsed)
	}
}
