package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.spongycastle.math.ec.ECPoint

/**
 * セッション確立要求に対する応答を表現するメッセージです。
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class AuthResponseMessage(val ephemeralPublicKey: ECPoint, val nonce: ImmutableBytes, val isTokenUsed: Boolean) {

	def encode: Array[Byte] = {
		val buffer = new Array[Byte](AuthResponseMessage.length)
		var offset = 0
		val publicBytes = ephemeralPublicKey.getEncoded(false)
		System.arraycopy(publicBytes, 1, buffer, offset, publicBytes.length - 1)
		offset += publicBytes.length - 1
		nonce.copyTo(0, buffer, offset, nonce.length)
		offset += nonce.length
		buffer(offset) = (if (isTokenUsed) 0x01 else 0x00).toByte
		offset += 1
		buffer
	}

	override def toString: String = "AuthResponseMessage"

}

object AuthResponseMessage {

	val length = 64 + 32 + 1

	def decode(wire: Array[Byte]): AuthResponseMessage = {
		var offset = 0
		val bytes = new Array[Byte](65)
		System.arraycopy(wire, offset, bytes, 1, 64)
		offset += 64
		bytes(0) = 0x04

		val ephemeralPublicKey = ECKey.CURVE.getCurve.decodePoint(bytes)
		val nonce = new Array[Byte](32)
		System.arraycopy(wire, offset, nonce, 0, 32)
		val immutableNonce = ImmutableBytes(nonce)

		offset += immutableNonce.length
		val tokenUsed = wire(offset)
		offset += 1
		if (tokenUsed != 0x00 && tokenUsed != 0x01) throw new RuntimeException("invalid boolean")
		val isTokenUsed = tokenUsed == 0x01

		new AuthResponseMessage(ephemeralPublicKey, immutableNonce, isTokenUsed)
	}
}
