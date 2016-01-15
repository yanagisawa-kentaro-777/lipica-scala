package org.lipicalabs.lipica.core.crypto.digest

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ダイジェスト値を表す trait です。
 *
 * ImmutableBytes の薄いラッパーです。
 *
 * Created by IntelliJ IDEA.
 * 2016/01/15 15:47
 * YANAGISAWA, Kentaro
 */
trait DigestValue extends Comparable[DigestValue] {

	/**
	 * このダイジェスト値を表すバイト数。
	 */
	def bytes: ImmutableBytes

	def toByteArray: Array[Byte] = this.bytes.toByteArray

	/**
	 * ダイジェスト値のバイト数。
	 */
	def length: Int = this.bytes.length

	def isEmpty: Boolean = this.length == 0

	override def hashCode: Int = this.bytes.hashCode

	override def equals(o: Any): Boolean = {
		try {
			this.bytes == o.asInstanceOf[DigestValue].bytes
		} catch {
			case any: Throwable => false
		}
	}

	override def compareTo(o: DigestValue): Int = {
		this.bytes.compareTo(o.bytes)
	}

	def toShortString: String = this.bytes.toShortString

	def toHexString: String = this.bytes.toHexString

	override def toString: String = this.toHexString

}

object DigestValue {
	/**
	 * テスト用、デバッグ用のメソッドです。
	 */
	def parseHexString(s: String): DigestValue = {
		new DigestValue {
			override val bytes = ImmutableBytes.parseHexString(s)
		}
	}

	/**
	 * テスト用、デバッグ用のメソッドです。
	 */
	def apply(aBytes: ImmutableBytes): DigestValue = {
		new DigestValue {
			override val bytes = aBytes
		}
	}

}

object EmptyDigest extends DigestValue {
	override val bytes = ImmutableBytes.empty
}

/**
 * 256ビットダイジェスト値を表すクラスです。
 */
class Digest256 private(override val bytes: ImmutableBytes) extends DigestValue

object Digest256 {

	private val NumberOfBytes = 32

	def apply(bytes: ImmutableBytes): Digest256 = {
		if (bytes.length != NumberOfBytes) {
			//256ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Digest256(bytes)
	}

	def apply(bytes: Array[Byte]): Digest256 = {
		if (bytes.length != NumberOfBytes) {
			//256ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Digest256(ImmutableBytes(bytes))
	}
}

/**
 * 512ビットダイジェスト値を表すクラスです。
 */
class Digest512 private(override val bytes: ImmutableBytes) extends DigestValue

object Digest512 {
	private val NumberOfBytes = 64

	def apply(bytes: ImmutableBytes): Digest512 = {
		if (bytes.length != NumberOfBytes) {
			//512ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Digest512(bytes)
	}

	def apply(bytes: Array[Byte]): Digest512 = {
		if (bytes.length != NumberOfBytes) {
			//512ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Digest512(ImmutableBytes(bytes))
	}
}