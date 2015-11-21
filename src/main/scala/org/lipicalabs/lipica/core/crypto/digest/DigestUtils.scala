package org.lipicalabs.lipica.core.crypto.digest

import java.security.MessageDigest

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}
import org.spongycastle.crypto.Digest
import org.spongycastle.crypto.digests.{KeccakDigest, RIPEMD160Digest}

object DigestUtils {

	val EmptyDataHash = ImmutableBytes.empty.keccak256
	val EmptySeqHash = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq.empty[ImmutableBytes]).keccak256
	val EmptyTrieHash = RBACCodec.Encoder.encode(ImmutableBytes.empty).keccak256

	private def calculateDigest(digest: Digest, data: Array[Byte]): Array[Byte] = {
		val result = new Array[Byte](digest.getDigestSize)
		if (data.nonEmpty) {
			digest.update(data, 0, data.length)
		}
		digest.doFinal(result, 0)
		result
	}

	private def keccakDigest(bits: Int, data: Array[Byte]): Array[Byte] = {
		val digest = new KeccakDigest(bits)
		calculateDigest(digest, data)
	}

	def keccak256(data: Array[Byte]): Array[Byte] = {
		keccakDigest(256, data)
	}

	def keccak512(data: Array[Byte]): Array[Byte] = {
		keccakDigest(512, data)
	}

	def keccak256omit12Bytes(data: Array[Byte]): Array[Byte] = {
		val digest = keccak256(data)
		java.util.Arrays.copyOfRange(digest, 12, digest.length)
	}

	def keccak256omit12(data: Array[Byte]): ImmutableBytes = {
		val digest = keccak256(data)
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
		calculateDigest(digest, data)
	}

	def computeNewAddress(address: ImmutableBytes, nonce: ImmutableBytes): ImmutableBytes = {
		val encodedSender = RBACCodec.Encoder.encode(address)
		val encodedNonce = RBACCodec.Encoder.encode(nonce.toPositiveBigInt)
		keccak256omit12(RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedSender, encodedNonce)).toByteArray)
	}

}
