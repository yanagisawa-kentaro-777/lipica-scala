package org.lipicalabs.lipica.core.vm

import java.nio.ByteBuffer

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.{Address160, Address}
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes, ByteUtils}

/**
 * Lipica の VM における１ワードである
 * 32バイト＝256ビットの不変の数値を表すクラスです。
 *
 * @since 2015, Oct. 17
 * @author YANAGISAWA, Kentaro
 */
class VMWord private(val data: ImmutableBytes) extends Comparable[VMWord] {

	import VMWord._

	/**
	 * 末尾20バイトを返します。
	 */
	def last20Bytes: Address = {
		Address160(this.data.copyOfRange(NumberOfBytes - 20, data.length))
	}

	/**
	 * 先頭のゼロを剥ぎとったバイト列を返します。
	 * @return
	 */
	def getDataWithoutLeadingZeros: ImmutableBytes = ByteUtils.stripLeadingZeroes(this.data)

	/**
	 * 正の整数としての値を返します。
	 */
	def value: BigInt = this.data.toPositiveBigInt

	/**
	 * 符号付き整数としての値を返します。
	 */
	def sValue: BigInt = this.data.toSignedBigInt

	/**
	 * この値の256ビットダイジェスト値を返します。
	 */
	def computeDigest256OfData: DigestValue = this.data.digest256

	/**
	 * この値をInt値として返します。
	 */
	def intValue: Int = {
		var result = 0
		this.data.indices.foreach {
			i => {
				result = (result << 8) + (data(i) & 0xFF)
			}
		}
		result
	}

	/**
	 * この値をInt値として返します。
	 * ただし、オーバーフロー時に Int.MaxValueを返します。
	 */
	def intValueSafe: Int = {
		if (4 < occupiedBytes) {
			return Int.MaxValue
		}
		val result = intValue
		if (result < 0) {
			Int.MaxValue
		} else {
			result
		}
	}

	/**
	 * この値をLong値として返します。
	 */
	def longValue: Long = {
		var result = 0L
		this.data.indices.foreach {
			i => {
				result = (result << 8) + (data(i) & 0xFF)
			}
		}
		result
	}

	/**
	 * この値をLong値として返します。
	 * ただし、オーバーフロー時に Long.MaxValueを返します。
	 */
	def longValueSafe: Long = {
		if (8 < occupiedBytes) {
			return Long.MaxValue
		}
		val result = longValue
		if (result < 0L) {
			Long.MaxValue
		} else {
			result
		}
	}


	def isZero: Boolean = {
		this.data.indices.foreach {
			i => {
				if (this.data(i) != 0) return false
			}
		}
		true
	}

	def `&`(another: VMWord): VMWord = {
		val result = new Array[Byte](NumberOfBytes)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) & another.data(i)).toByte
			}
		}
		VMWord(result)
	}

	def `|`(another: VMWord): VMWord = {
		val result = new Array[Byte](NumberOfBytes)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) | another.data(i)).toByte
			}
		}
		VMWord(result)
	}

	/**
	 * XOR。
	 */
	def `^`(another: VMWord): VMWord = {
		val result = new Array[Byte](NumberOfBytes)
		this.data.indices.foreach {
			i => {
				result(i) = (this.data(i) ^ another.data(i)).toByte
			}
		}
		VMWord(result)
	}

	/**
	 * １の補数（＝ビット反転）。
	 */
	def unary_~ = {
		VMWord(MaxValue - this.value)
	}

	/**
	 * 加算。
	 */
	def `+`(another: VMWord): VMWord = {
		val result = new Array[Byte](NumberOfBytes)

		var i = NumberOfBytes - 1
		var overflow = 0
		while (0 <= i) {
			val v = (this.data(i) & 0xff) + (another.data(i) & 0xff) + overflow
			result(i) = v.toByte
			overflow = v >>> 8
			i -= 1
		}
		VMWord(result)
	}

	/**
	 * 乗算。
	 */
	def `*`(another: VMWord): VMWord = {
		//TODO 性能。
		val result = this.value * another.value
		VMWord(result & MaxValue)
	}

	/**
	 * 減算。
	 */
	def `-`(another: VMWord): VMWord = {
		//TODO 性能。
		val result = this.value - another.value
		VMWord(result & MaxValue)
	}

	/**
	 * 整数除算。
	 */
	def `/`(another: VMWord): VMWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val result = this.value / another.value
		VMWord(result & MaxValue)
	}

	/**
	 * 符号付き整数除算。
	 */
	def sDiv(another: VMWord): VMWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val dividend = this.sValue
		val divisor = another.sValue
		val result = dividend / divisor
		val array = ByteUtils.asSignedByteArray(result & MaxValue, NumberOfBytes)
		VMWord(array)
	}

	/**
	 * 剰余演算。
	 */
	def `%`(another: VMWord): VMWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val result = this.value % another.value
		VMWord(result & MaxValue)
	}

	/**
	 * 符号付き剰余演算。
	 */
	def sMod(another: VMWord): VMWord = {
		//TODO 性能。
		if (another.isZero) {
			return Zero
		}
		val dividend = this.sValue
		val divisor = another.sValue
		val result = dividend % divisor
		val array = ByteUtils.asSignedByteArray(result & MaxValue, NumberOfBytes)
		VMWord(array)
	}

	/**
	 * べき乗演算。
	 */
	def exp(another: VMWord): VMWord = {
		//TODO 性能。
		VMWord(this.value.modPow(another.value, MAX_PLUS_ONE))
	}

	/**
	 * 加算＆剰余演算。
	 */
	def addMod(another1: VMWord, another2: VMWord): VMWord = {
		if (another2.isZero) {
			return Zero
		}
		(this + another1) % another2
	}

	/**
	 * 乗算＆剰余演算。
	 */
	def mulMod(another1: VMWord, another2: VMWord): VMWord = {
		if (another2.isZero) {
			return Zero
		}
		(this * another1) % another2
	}

	def signExtend(k: Int): VMWord = {
		if ((k < 0) || (NumberOfBytes <= k)) {
			throw new IndexOutOfBoundsException
		}
		val mask: Byte =
			if (this.sValue.testBit((k * 8) + 7)) {
				0xff.asInstanceOf[Byte]
			} else {
				0
			}
		val newArray = this.data.toByteArray
		((NumberOfBytes - 1) until k by -1).foreach {
			i => {
				newArray(newArray.length - 1 - i) = mask
			}
		}
		new VMWord(ImmutableBytes(newArray))
	}

	def occupiedBytes: Int = {
		val idx = this.data.firstIndex(_ != 0)
		if (idx < 0) {
			//ゼロでないバイトがないのだから、占有されているバイトはゼロバイトである。
			0
		} else {
			//ゼロでない最初のバイトから終端までが、専有されているバイトである。
			NumberOfBytes - idx
		}
	}

	override def equals(any: Any): Boolean = {
		any match {
			case null => false
			case another: VMWord => this.data == another.data
			case _ => false
		}
	}

	override def hashCode: Int = this.data.hashCode

	override def compareTo(another: VMWord): Int = {
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

object VMWord {

	val NumberOfBytes = 32
	val Zero = VMWord.apply(new Array[Byte](NumberOfBytes))
	val One = VMWord(1)
	private val MAX_PLUS_ONE: BigInt = BigInt(2).pow(NumberOfBytes * 8)
	val MaxValue: BigInt = MAX_PLUS_ONE - UtilConsts.One

	def wrap(src: Array[Byte]): ImmutableBytes = {
		if (src eq null) {
			ImmutableBytes.create(NumberOfBytes)
		} else if (src.length <= NumberOfBytes) {
			ImmutableBytes.expand(src, 0, src.length, NumberOfBytes)
		} else {
			throw new IllegalArgumentException("Byte array too long: %d < %d".format(NumberOfBytes, src.length))
		}
	}

	def apply(data: ImmutableBytes): VMWord = {
		if ((data eq null) || data.isEmpty) {
			Zero
		} else if (data.length == NumberOfBytes) {
			new VMWord(data)
		} else {
			new VMWord(wrap(data.toByteArray))
		}
	}

	def apply(src: Array[Byte]): VMWord = new VMWord(wrap(src))

	def apply(buffer: ByteBuffer): VMWord = VMWord.apply(buffer.array)

	def apply(value: Int): VMWord = {
		VMWord(ByteBuffer.allocate(4).putInt(value))
	}

	def apply(value: Long): VMWord = {
		VMWord(ByteBuffer.allocate(8).putLong(value))
	}

	def apply(s: String): VMWord = {
		VMWord(Hex.decodeHex(s.toCharArray))
	}

	def apply(value: BigInt): VMWord = {
		VMWord(ByteUtils.asUnsignedByteArray(value))
	}

	/**
	 * 渡されたバイト数が、何ワードを消費するか計算して返します。
	 */
	def countWords(bytes: Int): Int = {
		(bytes + NumberOfBytes - 1) / NumberOfBytes
	}

	/**
	 * 渡されたバイト数が、何ワードを消費するか計算して返します。
	 */
	def countWords(bytes: Long): Long = {
		(bytes + NumberOfBytes - 1) / NumberOfBytes
	}

}