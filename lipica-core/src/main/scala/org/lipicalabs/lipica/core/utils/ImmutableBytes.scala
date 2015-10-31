package org.lipicalabs.lipica.core.utils

import org.apache.commons.codec.binary.Hex

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

	def apply(index: Int): Byte = this.bytes(index)

	def asPositiveInt(index: Int): Int = this.bytes(index) & 0xFF

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
	override def toString: String = Hex.encodeHexString(this.bytes)
}

object ImmutableBytes {

	val empty = new ImmutableBytes(Array.emptyByteArray)

	def apply(bytes: Array[Byte]): ImmutableBytes = {
		if (ByteUtils.isNullOrEmpty(bytes)) {
			empty
		} else {
			new ImmutableBytes(java.util.Arrays.copyOf(bytes, bytes.length))
		}
	}

}