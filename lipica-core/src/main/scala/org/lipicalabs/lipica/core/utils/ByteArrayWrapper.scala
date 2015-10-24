package org.lipicalabs.lipica.core.utils

import org.apache.commons.codec.binary.Hex

/**
 * バイト配列のラッパークラスです。
 */
case class ByteArrayWrapper(data: Array[Byte]) extends Comparable[ByteArrayWrapper] with Serializable {

	override val hashCode: Int = java.util.Arrays.hashCode(this.data)

	override def equals(o: Any): Boolean = {
		try {
			val anotherData = o.asInstanceOf[ByteArrayWrapper].data
			this.data sameElements anotherData
		} catch {
			case e: Throwable => false
		}
	}

	override def compareTo(another: ByteArrayWrapper): Int = {
		ByteUtils.compareBytes(this.data, another.data)
	}

	override def toString: String = {
		Hex.encodeHexString(this.data)
	}

}
