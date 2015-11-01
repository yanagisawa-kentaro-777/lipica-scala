package org.lipicalabs.lipica.core.utils

object ByteUtils {

	/**
	 * ２個のバイト配列が先頭から一致するバイト数を返します。
	 */
	def matchingLength(a: Array[Byte], b: Array[Byte]): Int = {
		var result = 0
		val len = a.length.min(b.length)
		while (result < len) {
			if (a(result) != b(result)) {
				return result
			}
			result += 1
		}
		result
	}

	def matchingLength(a: ImmutableBytes, b: Array[Byte]): Int = {
		var result = 0
		val len = a.length.min(b.length)
		while (result < len) {
			if (a(result) != b(result)) {
				return result
			}
			result += 1
		}
		result
	}

	def matchingLength(a: ImmutableBytes, b: ImmutableBytes): Int = {
		var result = 0
		val len = a.length.min(b.length)
		while (result < len) {
			if (a(result) != b(result)) {
				return result
			}
			result += 1
		}
		result
	}
	
	/**
	 * 渡されたバイト配列から、先頭の連続するゼロを除外した
	 * バイト配列を返します。
	 */
	def stripLeadingZeroes(data: ImmutableBytes): ImmutableBytes = {
		val from = data.firstIndex(_ != 0)
		from match {
			case -1 =>
				ImmutableBytes.empty
			case 0 =>
				data
			case _ =>
				data.copyOfRange(from, data.length)
		}
	}

	def bigIntegerToBytes(b: java.math.BigInteger, numBytes: Int): Array[Byte] = {
		if (b == null) return null
		val bytes  = new Array[Byte](numBytes)
		val biBytes  = b.toByteArray
		val start = if (biBytes.length == numBytes + 1) 1 else 0
		val length = Math.min(biBytes.length, numBytes)
		System.arraycopy(biBytes, start, bytes, numBytes - length, length)
		bytes
	}


	/**
	 * 渡された BigInt 値から、符号の情報を剥ぎとって
	 * 絶対値を表すバイト配列を返します。
	 */
	def asUnsignedByteArray(value: BigInt): Array[Byte] = {
		if (value eq null) {
			return Array.emptyByteArray
		}
		val bytes = value.toByteArray
		if (bytes(0) == 0) {
			//正負の区別が不要なので、１バイト切り詰める。
			val result = new Array[Byte](bytes.length - 1)
			System.arraycopy(bytes, 1, result, 0, result.length)
			result
		} else {
			bytes
		}
	}

	/**
	 * BigIntを単純にバイト配列に変換して返します。
	 */
	def toByteArray(value: BigInt): Array[Byte] = {
		if (value eq null) {
			Array.emptyByteArray
		} else {
			value.toByteArray
		}
	}

	/**
	 * 渡されたバイト配列を、
	 * 渡された長さの配列にコピーして返します。
	 */
	def asSignedByteArray(value: BigInt, len: Int): Array[Byte] = {
		val src = asUnsignedByteArray(value)
		val dest = new Array[Byte](len)
		System.arraycopy(src, 0, dest, dest.length - src.length, src.length)
		dest
	}

	def compareBytes(a: Array[Byte], b: Array[Byte]): Int = {
		//TODO guava に速い実装があるらしい。
		BigInt(1, a).compare(BigInt(1, b))
	}

	def setBit(data: Array[Byte], pos: Int, positive: Boolean): Unit = {
		if ((data.length * 8) - 1 < pos) throw new IndexOutOfBoundsException("%d < %d ".format((data.length * 8) - 1, pos))

		val posByte = data.length - 1 - pos / 8
		val posBit = pos % 8
		val setter = (1 << posBit).toByte
		val toBeSet = data(posByte)
		val result =
			if (positive) (toBeSet | setter).toByte
			else (toBeSet & ~setter).toByte

		data(posByte) = result
	}

	def oneByteToHexString(value: Byte): String = {
		val result = Integer.toString(value & 0xFF, 16)
		if (result.length == 1) "0" + result else result
	}

	def isNullOrEmpty(bytes: Array[Byte]): Boolean = {
		(bytes eq null) || bytes.isEmpty
	}

	def isNullOrEmpty(bytes: ImmutableBytes): Boolean = {
		(bytes eq null) || bytes.isEmpty
	}

	def launderNullToEmpty(bytes: Array[Byte]): Array[Byte] = {
		if (bytes eq null) {
			Array.emptyByteArray
		} else {
			bytes
		}
	}

}
