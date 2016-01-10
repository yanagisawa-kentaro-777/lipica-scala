package org.lipicalabs.lipica.core.kernel

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

	private def get(array: Array[Byte], idx: Int): Byte = {
		if (idx < array.length) {
			array(idx)
		} else {
			0
		}
	}

	def `|`(another: Bloom): Bloom = {
		val newData = new Array[Byte](this.data.length max another.data.length)
		newData.indices.foreach { i =>
			newData(i) = (get(this.data, i) | get(another.data, i)).toByte
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
		/*

	for i := 0; i < 6; i += 2 {
		t := big.NewInt(1)
		b := (uint(b[i+1]) + (uint(b[i]) << 8)) & 2047
		r.Or(r, t.Lsh(t, b))
	}
		 */
		val mov1 = (((toBloom(0) & 0xff) << 8) + (toBloom(1) & 0xff)) & 2047
		val mov2 = (((toBloom(2) & 0xff) << 8) + (toBloom(3) & 0xff)) & 2047
		val mov3 = (((toBloom(4) & 0xff) << 8) + (toBloom(5) & 0xff)) & 2047

		val data = new Array[Byte](256)
		ByteUtils.setBit(data, mov1, positive = true)
		ByteUtils.setBit(data, mov2, positive = true)
		ByteUtils.setBit(data, mov3, positive = true)

		//println("%d %d %d".format(mov1, mov2, mov3))

		Bloom.apply(data)
	}

}