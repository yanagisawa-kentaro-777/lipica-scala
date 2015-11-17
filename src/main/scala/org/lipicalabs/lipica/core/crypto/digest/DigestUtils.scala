package org.lipicalabs.lipica.core.crypto.digest

import java.security.MessageDigest

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}
import org.spongycastle.crypto.digests.{KeccakDigest, SHA3Digest, RIPEMD160Digest}

object DigestUtils {

	val EmptyDataHash = ImmutableBytes.empty.sha3
	val EmptyTrieHash = RBACCodec.Encoder.encode(ImmutableBytes.empty).sha3

	def sha3(data: Array[Byte]): Array[Byte] = {
		val result = new Array[Byte](32)
		val digest = new KeccakDigest(256)
		if (data.nonEmpty) {
			digest.update(data, 0, data.length)
		}
		digest.doFinal(result, 0)
		result
	}

	def sha512(data: Array[Byte]): Array[Byte] = {
		val result = new Array[Byte](64)
		val digest = new SHA3Digest(512)
		if (data.nonEmpty) {
			digest.update(data, 0, data.length)
		}
		digest.doFinal(result, 0)
		result
	}

	def sha3omit12Bytes(data: Array[Byte]): Array[Byte] = {
		val digest = sha3(data)
		java.util.Arrays.copyOfRange(digest, 12, digest.length)
	}

	def sha3omit12(data: Array[Byte]): ImmutableBytes = {
		val digest = sha3(data)
		ImmutableBytes(digest, 12, digest.length)
	}

	def sha256(data: Array[Byte]): Array[Byte] = {
		try {
			val digest = MessageDigest.getInstance("SHA-256")
			digest.digest(data)
		} catch {
			case any: Throwable => Array.emptyByteArray
		}
	}

	def ripemd160(data: Array[Byte]): Array[Byte] = {
		val digest = new RIPEMD160Digest
		val result = new Array[Byte](digest.getDigestSize)
		digest.update(data, 0, data.length)
		digest.doFinal(result, 0)
		result
	}

	def computeNewAddress(address: ImmutableBytes, nonce: ImmutableBytes): ImmutableBytes = {
		val encodedSender = RBACCodec.Encoder.encode(address)
		val encodedNonce = RBACCodec.Encoder.encode(nonce.toPositiveBigInt)
		sha3omit12(RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedSender, encodedNonce)).toByteArray)
	}

}
