package org.lipicalabs.lipica.core.kernel

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, ByteUtils}

/**
 * ビットの並びを利用して、空間効率高く、要素の集合を表現するデータ構造の実装クラスです。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class BloomFilter private(private val data: Array[Byte]) {

	import BloomFilter._

	/**
	 * この BloomFilter を表すバイト列を返します。
	 */
	val immutableBytes: ImmutableBytes = ImmutableBytes(this.data)

	/**
	 * このブルームフィルタが、渡された値を含むか否かを返します。
	 */
	def contains(value: ImmutableBytes): Boolean = {
		val another = BloomFilter.createFromDigest(value.digest256)
		val newData = new Array[Byte](this.data.length max another.data.length)
		newData.indices.foreach { i =>
			newData(i) = (get(this.data, i) & get(another.data, i)).toByte
		}
		java.util.Arrays.equals(another.data, newData)
	}

	/**
	 * このBloomFilterと渡されたBloomFilterとの和を返します。
	 */
	def `|`(another: BloomFilter): BloomFilter = {
		val newData = new Array[Byte](this.data.length max another.data.length)
		newData.indices.foreach { i =>
			newData(i) = (get(this.data, i) | get(another.data, i)).toByte
		}
		new BloomFilter(newData)
	}

	override def toString: String = Hex.encodeHexString(this.data)

}

object BloomFilter {

	private val NumBytes = 256

	/**
	 * 渡されたバイト列を、そのままビット列とするBloomFilterを生成して返します。
	 */
	def apply(data: Array[Byte]): BloomFilter = {
		new BloomFilter(java.util.Arrays.copyOf(data, NumBytes))
	}

	/**
	 * 何も登録されていない新たな BloomFilter を生成して返します。
	 */
	def apply(): BloomFilter = {
		BloomFilter.apply(new Array[Byte](NumBytes))
	}

	/**
	 * 何も登録されていない新たな BloomFilter を生成して返します。
	 */
	def newInstance: BloomFilter = apply()

	/**
	 * ダイジェスト値（エントロピーが非常に高い）を受け取って、
	 * それに基いて３ビットを立てることによって、2048ビットのBloomFilterを生成して返します。
	 */
	def createFromDigest(aDigest: DigestValue): BloomFilter = {
		val digest = aDigest.bytes
		//渡されたダイジェスト値の最初の６バイトから、３個の整数（2047未満）を取り出す。
		val mov1 = (((digest(0) & 0xff) << 8) + (digest(1) & 0xff)) & 2047
		val mov2 = (((digest(2) & 0xff) << 8) + (digest(3) & 0xff)) & 2047
		val mov3 = (((digest(4) & 0xff) << 8) + (digest(5) & 0xff)) & 2047

		//256バイト＝2048ビットの中から、上で選択された３個のビットを立てる。
		val data = new Array[Byte](NumBytes)
		ByteUtils.setBit(data, mov1, positive = true)
		ByteUtils.setBit(data, mov2, positive = true)
		ByteUtils.setBit(data, mov3, positive = true)

		BloomFilter.apply(data)
	}

	private def get(array: Array[Byte], idx: Int): Byte = {
		if (idx < array.length) {
			array(idx)
		} else {
			0
		}
	}

}