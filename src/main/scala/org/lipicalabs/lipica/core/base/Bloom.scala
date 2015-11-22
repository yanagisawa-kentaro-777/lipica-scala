package org.lipicalabs.lipica.core.base

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}

/**
 * バイト配列を利用してビットの並びを表現するデータ構造の実装です。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Bloom private(private val data: Array[Byte]) {

	val immutableBytes: ImmutableBytes = ImmutableBytes(this.data)

	def copyData: Array[Byte] = {
		java.util.Arrays.copyOf(this.data, this.data.length)
	}

	def `|`(another: Bloom): Bloom = {
		val newData = copyData
		this.data.indices.foreach { i =>
			newData(i) = (this.data(i) | another.data(i)).toByte
		}
		new Bloom(newData)
	}

	override def toString: String = Hex.encodeHexString(this.data)

}

object Bloom {

	private val _8STEPS: Int = 8
	private val _3LOW_BITS: Int = 7
	private val ENSURE_BYTE: Int = 255

	def apply(data: Array[Byte]): Bloom = {
		new Bloom(data)
	}

	def apply(): Bloom = {
		Bloom.apply(new Array[Byte](256))
	}

	def create(toBloom: ImmutableBytes): Bloom = {
		val mov1 = (((toBloom(0) & ENSURE_BYTE) & _3LOW_BITS) << _8STEPS) + (toBloom(1) & ENSURE_BYTE)
		val mov2 = (((toBloom(2) & ENSURE_BYTE) & _3LOW_BITS) << _8STEPS) + (toBloom(3) & ENSURE_BYTE)
		val mov3 = (((toBloom(4) & ENSURE_BYTE) & _3LOW_BITS) << _8STEPS) + (toBloom(5) & ENSURE_BYTE)

		val data = new Array[Byte](256)
		ByteUtils.setBit(data, mov1, positive = true)
		ByteUtils.setBit(data, mov2, positive = true)
		ByteUtils.setBit(data, mov3, positive = true)

		Bloom.apply(data)
	}

}