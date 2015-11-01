package org.lipicalabs.lipica.core.vm

import java.nio.ByteBuffer

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}

/**
 * 32バイト＝256ビットの数値を表すクラスです。
 *
 * @since 2015, Oct. 17
 * @author YANAGISAWA, Kentaro
 */
class DataWord private(val data: ImmutableBytes) extends Comparable[DataWord] {

	import DataWord._

	def last20Bytes: ImmutableBytes = this.data.copyOfRange(NUM_BYTES - 20, data.length)
	def getDataWithoutLeadingZeros: ImmutableBytes = ByteUtils.stripLeadingZeroes(this.data)

	def value: BigInt = this.data.toPositiveBigInt
	def sValue: BigInt = this.data.toSignedBigInt

	def computeSHA3OfData: ImmutableBytes = this.data.sha3

	def intValue: Int = {
		var result = 0
		this.data.indices.foreach {
			i => {
				result = (result << 8) + (data(i) & 0xFF)
			}
		}
		result
	}

	def longValue: Long = {
		var result = 0L
		this.data.indices.foreach {
			i => {
				result = (result << 8) + (data(i) & 0xFF)
			}
		}
		result
	}

	def isZero: Boolean = {
		this.data.indices.foreach {
			i => {
				if (this.data(i) != 0) return false
			}
		}
		true
	}

	def `&`(another: DataWord): DataWord = {
		val result = new Array[Byte](NUM_BYTES)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) & another.data(i)).toByte
			}
		}
		DataWord(result)
	}

	def `|`(another: DataWord): DataWord = {
		val result = new Array[Byte](NUM_BYTES)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) | another.data(i)).toByte
			}
		}
		DataWord(result)
	}

	/**
	 * XOR。
	 */
	def `^`(another: DataWord): DataWord = {
		val result = new Array[Byte](NUM_BYTES)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) ^ another.data(i)).toByte
			}
		}
		DataWord(result)
	}

	/**
	 * １の補数（＝ビット反転）。
	 */
	def unary_~ = {
		DataWord(MaxValue - this.value)
	}

	/**
	 * 加算。
	 */
	def `+`(another: DataWord): DataWord = {
		val result = new Array[Byte](NUM_BYTES)

		var i = NUM_BYTES - 1
		var overflow = 0
		while (0 <= i) {
			val v = (this.data(i) & 0xff) + (another.data(i) & 0xff) + overflow
			result(i) = v.toByte
			overflow = v >>> 8
			i -= 1
		}
		DataWord(result)
	}

	/**
	 * 乗算。
	 */
	def `*`(another: DataWord): DataWord = {
		//TODO 性能。
		val result = this.value * another.value
		DataWord(result & MaxValue)
	}

	/**
	 * 減算。
	 */
	def `-`(another: DataWord): DataWord = {
		//TODO 性能。
		val result = this.value - another.value
		DataWord(result & MaxValue)
	}

	/**
	 * 整数除算。
	 */
	def `/`(another: DataWord): DataWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val result = this.value / another.value
		DataWord(result & MaxValue)
	}

	/**
	 * 符号付き整数除算。
	 */
	def sDiv(another: DataWord): DataWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val dividend = this.sValue
		val divisor = another.sValue
		val result = dividend / divisor
		val array = ByteUtils.asSignedByteArray(result & MaxValue, NUM_BYTES)
		DataWord(array)
	}

	/**
	 * 剰余演算。
	 */
	def `%`(another: DataWord): DataWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val result = this.value % another.value
		DataWord(result & MaxValue)
	}

	/**
	 * 符号付き剰余演算。
	 */
	def sMod(another: DataWord): DataWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val dividend = this.sValue
		val divisor = another.sValue
		val result = dividend % divisor
		val array = ByteUtils.asSignedByteArray(result & MaxValue, NUM_BYTES)
		DataWord(array)
	}

	/**
	 * べき乗演算。
	 */
	def exp(another: DataWord): DataWord = {
		//TODO 性能。
		DataWord(this.value.modPow(another.value, MAX_PLUS_ONE))
	}

	/**
	 * 加算＆剰余演算。
	 */
	def addMod(another1: DataWord, another2: DataWord): DataWord = {
		if (another2.isZero) {
			return Zero
		}
		(this + another1) % another2
	}

	/**
	 * 乗算＆剰余演算。
	 */
	def mulMod(another1: DataWord, another2: DataWord): DataWord = {
		if (another2.isZero) {
			return Zero
		}
		(this * another1) % another2
	}

	def signExtend(k: Int): DataWord = {
		if ((k < 0) || (NUM_BYTES <= k)) {
			throw new IndexOutOfBoundsException
		}
		val mask: Byte =
			if (this.sValue.testBit((k * 8) + 7)) {
				0xff.asInstanceOf[Byte]
			} else {
				0
			}
		val newArray = this.data.toByteArray
		((NUM_BYTES - 1) until k by -1).foreach {
			i => {
				newArray(newArray.length - 1 - i) = mask
			}
		}
		new DataWord(ImmutableBytes(newArray))
	}

	def occupiedBytes: Int = {
		val idx = this.data.firstIndex(_ != 0)
		if (idx < 0) {
			//ゼロでないバイトがないのだから、占有されているバイトはゼロバイトである。
			0
		} else {
			//ゼロでない最初のバイトから終端までが、専有されているバイトである。
			NUM_BYTES - idx
		}
	}

	override def equals(any: Any): Boolean = {
		any match {
			case null => false
			case another: DataWord => this.data == another.data
			case _ => false
		}
	}

	override def hashCode: Int = this.data.hashCode

	override def compareTo(another: DataWord): Int = {
		this.data compareTo another.data
	}

	override def toString: String = toHexString

	def toHexString: String = this.data.toHexString

	def toPrefixString: String = {
		val pref = getDataWithoutLeadingZeros
		if (pref.isEmpty) {
			return ""
		}
		val result =  pref.toHexString
		if (pref.length < 7) {
			result
		} else {
			result.substring(0, 6)
		}
	}

	def shortHex: String = "0x" + getDataWithoutLeadingZeros.toHexString.toUpperCase

	def isHex(s: String): Boolean = toHexString == s

}

object DataWord {

	val NUM_BYTES = 32
	val Zero = DataWord.apply(new Array[Byte](NUM_BYTES))
	val One = DataWord(1)
	private val MAX_PLUS_ONE: BigInt = BigInt(2).pow(NUM_BYTES * 8)
	val MaxValue: BigInt = MAX_PLUS_ONE - BigInt(1)

	def wrap(src: Array[Byte]): ImmutableBytes = {
		if (src eq null) {
			ImmutableBytes.create(NUM_BYTES)
		} else if (src.length <= NUM_BYTES) {
			ImmutableBytes.expand(src, 0, src.length, NUM_BYTES)
		} else {
			throw new IllegalArgumentException("Byte array too long: %d < %d".format(NUM_BYTES, src.length))
		}
	}

	def apply(data: ImmutableBytes): DataWord = new DataWord(data)

	def apply(src: Array[Byte]): DataWord = new DataWord(wrap(src))

	def apply(buffer: ByteBuffer): DataWord = DataWord.apply(buffer.array)

	def apply(value: Int): DataWord = {
		DataWord(ByteBuffer.allocate(4).putInt(value))
	}

	def apply(value: Long): DataWord = {
		DataWord(ByteBuffer.allocate(8).putLong(value))
	}

	def apply(s: String): DataWord = {
		DataWord(Hex.decodeHex(s.toCharArray))
	}

	def apply(value: BigInt): DataWord = {
		DataWord(ByteUtils.asUnsignedByteArray(value))
	}

}