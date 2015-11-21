package org.lipicalabs.lipica.core.utils

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.spongycastle.util.Arrays

/**
 * 不可変なバイト配列クラスの実装です。
 *
 * @since 2015/10/31
 * @author YANAGISAWA, Kentaro
 */
class ImmutableBytes private(private val bytes: Array[Byte]) extends Comparable[ImmutableBytes] {

	/** このバイト配列の長さを返します。 */
	val length: Int = this.bytes.length

	/** このバイト配列の長さを返します。 */
	val size: Int = this.length

	/** このバイト配列の添字を走査するためのRangeを返します。*/
	def indices: Range = this.bytes.indices

	/** このバイト配列の長さがゼロであれば真を返します。 */
	def isEmpty: Boolean = {
		this.length == 0
	}
	/** このバイト配列の長さがゼロでなければ真を返します。 */
	def nonEmpty: Boolean = {
		0 < this.length
	}

	def head: Byte = this.bytes.head
	def last: Byte = this.bytes.last

	/**
	 * 指定された添字の要素を返します。
	 */
	def apply(index: Int): Byte = this.bytes(index)

	/**
	 * 指定された添字の要素を、正の整数として返します。
	 */
	def asPositiveInt(index: Int): Int = this.bytes(index) & 0xFF

	/**
	 * このオブジェクトをArray[Byte]に変換して返します。
	 */
	def toByteArray: Array[Byte] = java.util.Arrays.copyOfRange(this.bytes, 0, this.bytes.length)

	/**
	 * このオブジェクトの内容を、渡されたバイト配列にコピーします。
	 */
	def copyTo(srcPos: Int, dest: Array[Byte], destPos: Int, len: Int): Unit = {
		System.arraycopy(this.bytes, srcPos, dest, destPos, len)
	}

	/**
	 * このオブジェクトの指定された範囲を、新たなインスタンスとして返します。
	 */
	def copyOfRange(from: Int, until: Int): ImmutableBytes = {
		new ImmutableBytes(java.util.Arrays.copyOfRange(this.bytes, from, until))
	}

	def copyOf(newLength: Int): ImmutableBytes = {
		new ImmutableBytes(java.util.Arrays.copyOf(this.bytes, newLength))
	}

	def `++`(another: ImmutableBytes): ImmutableBytes = {
		new ImmutableBytes(this.bytes ++ another.bytes)
	}

	def `:+`(oneByte: Byte): ImmutableBytes = {
		new ImmutableBytes(this.bytes :+ oneByte)
	}

	def `+:`(oneByte: Byte): ImmutableBytes = {
		new ImmutableBytes(oneByte +: this.bytes)
	}

	def reverse: ImmutableBytes = ImmutableBytes(Arrays.reverse(this.bytes))

	def digest256: ImmutableBytes = new ImmutableBytes(DigestUtils.digest256(this.bytes))
	def digest512: ImmutableBytes = new ImmutableBytes(DigestUtils.digest512(this.bytes))
	def sha2_256: ImmutableBytes = new ImmutableBytes(DigestUtils.sha2_256(this.bytes))
	def ripemd160(newLength: Int): ImmutableBytes = {
		val newData = DigestUtils.ripemd160(this.bytes)
		ImmutableBytes.expand(newData, 0, newData.length, newLength)
	}

	/**
	 * 渡された条件を満たす最初の添字を返します。
	 */
	def firstIndex(p: (Byte) => Boolean): Int = {
		this.bytes.indices.foreach {i => {
			if (p(this.bytes(i))) {
				return i
			}
		}}
		-1
	}

	/**
	 * 渡された条件を満たす要素の個数を返します。
	 */
	def count(p: (Byte) => Boolean): Int = this.bytes.count(p)

	/**
	 * このオブジェクトのバイト配列を、正のBigIntとして返します。
	 */
	def toPositiveBigInt: BigInt = BigInt(1, this.bytes)

	/**
	 * このオブジェクトのバイト配列を、符号付きのBigIntとして返します。
	 */
	def toSignedBigInt: BigInt = BigInt(this.bytes)

	/**
	 * このオブジェクトのバイト配列を、符号付きのBigIntegerとして返します。
	 */
	def toSignedBigInteger: java.math.BigInteger = new java.math.BigInteger(this.bytes)

	/**
	 * このオブジェクトのバイト配列を、正のLong値として返します。
	 */
	def toPositiveLong: Long = toPositiveBigInt.longValue()

	/**
	 * このバイト列を、指定されたキャラクターセットでエンコードされた文字列として返します。
	 * toString メソッドと異なり、十六進エンコーディングを行うわけではありません。
	 *
	 * @param charset キャラクタセット。
	 * @return 文字列。
	 */
	def asString(charset: Charset): String = asString(0, this.bytes.length, charset)

	/**
	 * このバイト列の中の特定の部分配列を、
	 * 指定されたキャラクターセットでエンコードされた文字列として返します。
	 * toString メソッドと異なり、十六進エンコーディングを行うわけではありません。
	 *
	 * @param offset 開始箇所の添字。
	 * @param len 文字列として解釈する長さ。
	 * @param charset キャラクタセット。
	 * @return 文字列。
	 */
	def asString(offset: Int, len: Int, charset: Charset): String = new String(this.bytes, offset, len, charset)

	override def compareTo(another: ImmutableBytes): Int = {
		//TODO guava に速い実装があるらしい。
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

	def sameElements(byteArray: Array[Byte]): Boolean = {
		java.util.Arrays.equals(this.bytes, byteArray)
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

	val zero = new ImmutableBytes(Array[Byte](0))

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

	def apply(stream: ByteArrayOutputStream): ImmutableBytes = {
		new ImmutableBytes(stream.toByteArray)
	}

	def apply(original: Array[Byte]): ImmutableBytes = {
		if (original eq null) {
			return empty
		}
		apply(original, 0, original.length)
	}

	def parseHexString(s: String): ImmutableBytes = {
		if (s eq null) {
			empty
		} else {
			apply(Hex.decodeHex(s.toCharArray))
		}
	}

	def fromOneByte(v: Byte): ImmutableBytes = {
		new ImmutableBytes(Array[Byte](v))
	}

	def asUnsignedByteArray(value: BigInt): ImmutableBytes = {
		new ImmutableBytes(ByteUtils.asUnsignedByteArray(value))
	}

	def asSignedByteArray(value: BigInt): ImmutableBytes = {
		new ImmutableBytes(ByteUtils.toByteArray(value))
	}

	def create(length: Int): ImmutableBytes = {
		if (length <= 0) {
			empty
		} else {
			new ImmutableBytes(new Array[Byte](length))
		}
	}

}