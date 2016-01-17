package org.lipicalabs.lipica.core.utils

/**
 * 正の整数を表現するバイト配列を表すクラスです。
 *
 * ImmutableBytes の薄いラッパーです。
 *
 * Created by IntelliJ IDEA.
 * 2016/01/15 17:43
 * YANAGISAWA, Kentaro
 */
class BigIntBytes private(val bytes: ImmutableBytes) extends Comparable[BigIntBytes] {

	val positiveBigInt: BigInt = this.bytes.toPositiveBigInt

	val toPositiveBigInt: BigInt = this.bytes.toPositiveBigInt

	def toByteArray: Array[Byte] = this.bytes.toByteArray

	/**
	 * アドレスのバイト数。
	 */
	def length: Int = this.bytes.length

	def isEmpty: Boolean = this.length == 0

	def increment: BigIntBytes = {
		BigIntBytes(this.positiveBigInt + 1)
	}

	override def hashCode: Int = this.bytes.hashCode

	override def equals(o: Any): Boolean = {
		try {
			this.bytes == o.asInstanceOf[BigIntBytes].bytes
		} catch {
			case any: Throwable => false
		}
	}

	override def compareTo(o: BigIntBytes): Int = {
		this.bytes.compareTo(o.bytes)
	}

	def toShortString: String = this.bytes.toShortString

	def toHexString: String = this.bytes.toHexString

	def toDecimalString: String = "%,d".format(this.positiveBigInt)

	override def toString: String = toDecimalString

}

object BigIntBytes {

	val zero: BigIntBytes = new BigIntBytes(ImmutableBytes.empty)

	val empty: BigIntBytes = new BigIntBytes(ImmutableBytes.empty)

	def apply(bytes: ImmutableBytes): BigIntBytes = {
		new BigIntBytes(bytes)
	}

	def apply(bytes: Array[Byte]): BigIntBytes = {
		new BigIntBytes(ImmutableBytes(bytes))
	}

	def apply(v: BigInt): BigIntBytes = {
		new BigIntBytes(ImmutableBytes.asUnsignedByteArray(v))
	}

	def parseHexString(s: String): BigIntBytes = {
		new BigIntBytes(ImmutableBytes.parseHexString(s))
	}

	private def increment(bytes: Array[Byte]): Boolean = {
		val startIndex = 0
		bytes.indices.reverse.foreach {
			i => {
				bytes(i) = (bytes(i) + 1).toByte
				if (bytes(i) != 0) {
					return true
				} else if (i == startIndex) {
					return false
				}
			}
		}
		true
	}
}






