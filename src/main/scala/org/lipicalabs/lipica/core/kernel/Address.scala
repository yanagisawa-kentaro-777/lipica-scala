package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * アカウントやコントラクトのアドレスを表現する trait です。
 *
 * ImmutableBytes の薄いラッパーです。
 *
 * Created by IntelliJ IDEA.
 * 2016/01/15 17:43
 * YANAGISAWA, Kentaro
 */
trait Address extends Comparable[Address] {

	/**
	 * このダイジェスト値を表すバイト数。
	 */
	def bytes: ImmutableBytes

	def toByteArray: Array[Byte] = this.bytes.toByteArray

	/**
	 * アドレスのバイト数。
	 */
	def length: Int = this.bytes.length

	def isEmpty: Boolean = this.length == 0

	override def hashCode: Int = this.bytes.hashCode

	override def equals(o: Any): Boolean = {
		try {
			this.bytes == o.asInstanceOf[Address].bytes
		} catch {
			case any: Throwable => false
		}
	}

	override def compareTo(o: Address): Int = {
		this.bytes.compareTo(o.bytes)
	}

	def toShortString: String = this.bytes.toShortString

	def toHexString: String = this.bytes.toHexString

	override def toString: String = this.toHexString

}

object Address {
	/**
	 * テスト用およびデバッグ用のメソッドです。
	 */
	def parseHexString(s: String): Address = {
		new Address {
			override val bytes = ImmutableBytes.parseHexString(s)
		}
	}

	/**
	 * テスト用およびデバッグ用のメソッドです。
	 */
	def apply(aBytes: ImmutableBytes): Address = {
		new Address {
			override val bytes = aBytes
		}
	}

	/**
	 * テスト用およびデバッグ用のメソッドです。
	 */
	def apply(aBytes: Array[Byte]): Address = {
		new Address {
			override val bytes = ImmutableBytes(aBytes)
		}
	}
}

object EmptyAddress extends Address {
	override val bytes = ImmutableBytes.empty
}

/**
 * 160ビット（＝20バイト）のアドレスを表現するクラスです。
 */
class Address160 private(override val bytes: ImmutableBytes) extends Address

object Address160 {

	private val NumberOfBytes = 20

	def apply(bytes: ImmutableBytes): Address160 = {
		if (bytes.length != NumberOfBytes) {
			//160ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Address160(bytes)
	}

	def apply(bytes: Array[Byte]): Address160 = {
		if (bytes.length != NumberOfBytes) {
			//160ビットでないものは受け入れない。
			throw new IllegalArgumentException("%,d != %,d".format(bytes.length, NumberOfBytes))
		}
		new Address160(ImmutableBytes(bytes))
	}

	def parseHexString(s: String): Address160 = {
		apply(ImmutableBytes.parseHexString(s))
	}

}
