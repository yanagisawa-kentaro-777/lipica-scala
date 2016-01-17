package org.lipicalabs.lipica.core.utils

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * 正の整数を表現する最小限の長さのバイト配列を表すクラスです。
 *
 * ImmutableBytes の薄いラッパーです。
 *
 * Created by IntelliJ IDEA.
 * 2016/01/15 17:43
 * YANAGISAWA, Kentaro
 */
class BigIntBytes private(val bytes: ImmutableBytes) extends Comparable[BigIntBytes] {

	val positiveBigInt: BigInt = this.bytes.toPositiveBigInt

	def toByteArray: Array[Byte] = this.bytes.toByteArray

	/**
	 * アドレスのバイト数。
	 */
	def length: Int = this.bytes.length

	def isEmpty: Boolean = this.length == 0

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

	def apply(bytes: ImmutableBytes): BigIntBytes = {
		new BigIntBytes(bytes)
	}

	def apply(bytes: Array[Byte]): BigIntBytes = {
		new BigIntBytes(ImmutableBytes(bytes))
	}

	def parseHexString(s: String): BigIntBytes = {
		new BigIntBytes(ImmutableBytes.parseHexString(s))
	}
}






