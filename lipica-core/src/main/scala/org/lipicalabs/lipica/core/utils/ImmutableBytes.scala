package org.lipicalabs.lipica.core.utils

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils

/**
 * 不可変なバイト配列クラスの実装です。
 *
 * @since 2015/10/31
 * @author YANAGISAWA, Kentaro
 */
class ImmutableBytes private(private val bytes: Array[Byte]) extends Comparable[ImmutableBytes] {

	val length: Int = this.bytes.length
	val size: Int = this.length
	def indices: Range = this.bytes.indices

	def isEmpty: Boolean = {
		this.length == 0
	}

	def apply(index: Int): Byte = this.bytes(index)

	def toByteArray: Array[Byte] = java.util.Arrays.copyOfRange(this.bytes, 0, this.bytes.length)

	def copyTo(srcPos: Int, dest: Array[Byte], destPos: Int, len: Int): Unit = {
		System.arraycopy(this.bytes, srcPos, dest, destPos, len)
	}

	def sha3: ImmutableBytes = new ImmutableBytes(DigestUtils.sha3(this.bytes))

	def firstIndex(p: (Byte) => Boolean): Int = {
		this.bytes.indices.foreach {i => {
			if (p(this.bytes(i))) {
				return i
			}
		}}
		-1
	}

	def asPositiveInt(index: Int): Int = this.bytes(index) & 0xFF

	def copyOfRange(from: Int, until: Int): ImmutableBytes = {
		new ImmutableBytes(java.util.Arrays.copyOfRange(this.bytes, from, until))
	}

	def toPositiveBigInt: BigInt = BigInt(1, this.bytes)

	def toSignedBigInt: BigInt = BigInt(this.bytes)

	override def compareTo(another: ImmutableBytes): Int = {
		val min = this.bytes.length.min(another.bytes.length)
		(0 until min).foreach {
			i => {
				val eachDiff = this.asPositiveInt(i) - another.asPositiveInt(i)
				if (eachDiff < 0) {
					return -1
				} else if (0 < eachDiff) {
					return 1
				}
			}
		}
		if (this.bytes.length < another.bytes.length) {
			-1
		} else if (another.bytes.length < this.bytes.length) {
			1
		} else {
			0
		}
	}

	override def hashCode: Int = java.util.Arrays.hashCode(this.bytes)

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[ImmutableBytes]
			if (this eq another) {
				return true
			}
			java.util.Arrays.equals(this.bytes, another.bytes)
		} catch {
			case any: Throwable => false
		}
	}

	def toHexString: String = Hex.encodeHexString(this.bytes)

	override def toString: String = toHexString
}

object ImmutableBytes {

	val empty = new ImmutableBytes(Array.emptyByteArray)

	def expand(original: Array[Byte], from: Int, until: Int, newLength: Int): ImmutableBytes = {
		if (newLength <= 0) {
			empty
		} else if (original eq null) {
			new ImmutableBytes(new Array[Byte](newLength))
		} else {
			val data = new Array[Byte](newLength)
			val len = until - from
			System.arraycopy(original, 0, data, newLength - len, len)
			new ImmutableBytes(data)
		}
	}

	def apply(original: Array[Byte], from: Int, until: Int): ImmutableBytes = {
		if (ByteUtils.isNullOrEmpty(original) || (until <= from)) {
			empty
		} else {
			val data = java.util.Arrays.copyOfRange(original, from, until)
			new ImmutableBytes(data)
		}
	}

	def apply(original: Array[Byte]): ImmutableBytes = {
		if (original eq null) {
			return empty
		}
		apply(original, 0, original.length)
	}

	def create(length: Int): ImmutableBytes = {
		if (length <= 0) {
			empty
		} else {
			new ImmutableBytes(new Array[Byte](length))
		}
	}

}