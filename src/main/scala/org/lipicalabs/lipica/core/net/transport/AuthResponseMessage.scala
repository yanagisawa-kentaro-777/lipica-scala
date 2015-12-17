package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.spongycastle.math.ec.ECPoint

/**
 *
 * @since 2015/12/17
 * @author YANAGISAWA, Kentaro
 */
class AuthResponseMessage {

	private var _ephemeralPublicKey: ECPoint = null//64 bytes.
	def ephemeralPublicKey: ECPoint = this._ephemeralPublicKey
	def ephemeralPublicKey_=(v: ECPoint): Unit = this._ephemeralPublicKey = v

	private var _nonce: ImmutableBytes = null//32 bytes.
	def nonce: ImmutableBytes = this._nonce
	def nonce_=(v: ImmutableBytes): Unit = this._nonce = v

	private var _isTokenUsed: Boolean = false//1 byte
	def isTokenUsed: Boolean = this._isTokenUsed
	def isTokenUsed_=(v: Boolean): Unit = this._isTokenUsed = v

	def encode: ImmutableBytes = {
		val buffer = new Array[Byte](AuthResponseMessage.length)
		var offset = 0
		val publicBytes = ephemeralPublicKey.getEncoded(false)
		System.arraycopy(publicBytes, 1, buffer, offset, publicBytes.length - 1)
		offset += publicBytes.length - 1
		nonce.copyTo(0, buffer, offset, nonce.length)
		offset += nonce.length
		buffer(offset) = (if (isTokenUsed) 0x01 else 0x00).toByte
		offset += 1
		ImmutableBytes(buffer)
	}

}

object AuthResponseMessage {

	val length = 34 + 32 + 1

	def decode(wire: Array[Byte]): AuthResponseMessage = {
		var offset = 0
		val message = new AuthResponseMessage
		val bytes = new Array[Byte](65)
		System.arraycopy(wire, offset, bytes, 1, 64)
		offset += 64
		bytes(0) = 0x04

		message.ephemeralPublicKey = ECKey.CURVE.getCurve.decodePoint(bytes)
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
