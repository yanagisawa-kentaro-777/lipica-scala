package org.lipicalabs.lipica.core.crypto.digest

import java.security.MessageDigest

import org.spongycastle.crypto.digests.RIPEMD160Digest

object DigestUtils {

	def sha3(data: Array[Byte]): Array[Byte] = {
		val digest = new Keccak256
		digest.update(data)
		digest.digest
	}

	def sha3omit12(data: Array[Byte]): Array[Byte] = {
		val digest = sha3(data)
		java.util.Arrays.copyOfRange(digest, 12, digest.length)
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

}
