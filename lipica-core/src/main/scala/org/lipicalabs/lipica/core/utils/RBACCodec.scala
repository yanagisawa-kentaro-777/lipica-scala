package org.lipicalabs.lipica.core.utils

import java.io.PrintStream
import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Hex

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer

/**
 * 入れ子になったバイト配列コンテナ（Recursive Byte Array Container）の
 * Codecです。
 *
 * @author YANAGISAWA, Kentaro
 */
object RBACCodec {

	/**
	 * Allow for content up to size of 2 power 64 bytes
	 */
	private val MAX_ITEM_LENGTH: Double = Math.pow(256, 8)

	/**
	 * Reason for threshold according to Vitalik Buterin:
	 * - 56 bytes maximizes the benefit of both options
	 * - if we went with 60 then we would have only had 4 slots for long strings
	 * so RLP would not have been able to store objects above 4gb
	 * - if we went with 48 then RLP would be fine for 2^128 space, but that's way too much
	 * - so 56 and 2^64 space seems like the right place to put the cutoff
	 * - also, that's where Bitcoin's varint does the cutof
	 */
	private val SIZE_THRESHOLD = 56

	/**
	 * [0x00, 0x7f] （すなわち127以下）の範囲の１バイトについては、
	 * そのバイト自身がエンコードされた表現となる。
	 */
	/**
	 * [0x80]
	 * バイト列が0バイト以上55バイト以下の場合、
	 * エンコードされた表現は、
	 * 0x80（すなわち128）にバイト列の長さを足した１バイトで開始され、
	 * その後にバイト列が続く。
	 * ゆえに最初のバイトは、[0x80, 0xb7]（すなわち128以上183以下）となる。
	 */
	private val OFFSET_SHORT_ITEM = 0x80

	/**
	 * [0xb7]
	 * バイト列が55バイトよりも長い場合、
	 * エンコードされた表現は、
	 * 0xb7（すなわち183）にバイト列の長さの長さを足した１バイトで開始され、
	 * 次にバイト列の長さ、そしてその後にバイト列が続く。
	 * たとえば1024バイトのバイト列は、0xb9, 0x04, 0x00 の３バイトで開始され、
	 * その後にバイト列本体が続く。
	 * ゆえに最初のバイトは、[0xb8, 0xbf]（すなわち184以上191以下）となる。
	 */
	private val OFFSET_LONG_ITEM = 0xb7

	/**
	 * [0xc0]
	 * リストのペイロード全体（すなわち、リストの個々の要素の長さをすべて合計した値）が
	 * 0バイト以上55バイト以下である場合、
	 * エンコードされた表現は、0xc0（すなわち192）にリストの容量を加えた１バイトに、
	 * エンコードされたリスト要素を結合したものをつなげたものとなる。
	 * したがって、最初のバイトは[0xc0, 0xf7] （すなわち192以上247以下）となる。
	 */
	private val OFFSET_SHORT_LIST = 0xc0

	/**
	 * [0xf7]
	 * リストのペイロード全体（すなわち、リストの個々の要素の長さをすべて合計した値）が
	 * 55バイトよりも大きい場合、
	 * エンコードされた表現は、0xf7（すなわち247）にリストの容量の長さを加えた１バイトに、
	 * リストの容量が続き、その後にリスト本体のエンコードされた表現が続くものとなる。
	 * したがって、最初のバイトは[0xf8, 0xf] （すなわち248以上255以下）となる。
	 */
	private val OFFSET_LONG_LIST = 0xf7

	private val BIGINT_ZERO = BigInt(0L)

	@tailrec
	private def countBytesRecursively(v: Long, accum: Int): Int = {
		if (v == 0L) return accum
		countBytesRecursively(v >>> 8, accum + 1)
	}

	private def countBytesOfNumber(v: Long): Int = {
		countBytesRecursively(v, 0)
	}

	private def encodeNumberInBigEndian(value: Long, length: Int): Array[Byte] = {
		val result = new Array[Byte](length)
		(0 until length).foreach {
			i => {
				result(length - 1 - i) = ((value >> (8 * i)) & 0xff).asInstanceOf[Byte]
			}
		}
		result
	}

	/**
	 * バイト数を節約して、整数をビッグエンディアンのバイト列に変換する。
	 */
	private def bytesFromInt(value: Int): Array[Byte] = {
		if (value == 0) return Array.empty
		val len = countBytesOfNumber(value)
		encodeNumberInBigEndian(value, len)
	}


	object Encoder {

		/**
		 * あらゆる値をエンコードします。
		 */
		def encode(v: Any): Array[Byte] = {
			v match {
				case seq: Seq[_] => encodeSeq(seq)
				case any => encodeItem(any)
			}
		}

		/**
		 * バイト配列の並びをエンコードします。
		 */
		def encodeSeqOfByteArrays(seq: Seq[Array[Byte]]): Array[Byte] = {
			if (seq eq null) {
				//要素なしのリストとする。
				return Array(OFFSET_SHORT_LIST.asInstanceOf[Byte])
			}
			//リスト要素の全ペイロードを合計する。
			val totalLength = seq.foldLeft(0)((accum, each) => accum + each.length)
			//リストのヘッダ部分を構築する。
			val (data: Array[Byte], initialPos: Int) =
				if (totalLength < SIZE_THRESHOLD) {
					//これは短いリストである。
					val d = new Array[Byte](1 + totalLength)
					d(0) = (OFFSET_SHORT_LIST + totalLength).asInstanceOf[Byte]
					(d, 1)
				} else {
					//これは長いリストである。
					//まずは、リストの容量を表現するのに何バイト必要かを数える。
					val byteNum = countBytesOfNumber(totalLength)
					//その結果を利用して、リストの容量をビッグエンディアンでエンコードする。
					val lengthBytes = encodeNumberInBigEndian(totalLength, byteNum)

					//ヘッダ部分を組み立てる。
					val d = new Array[Byte](1 + lengthBytes.length + totalLength)
					d(0) = (OFFSET_LONG_LIST + byteNum).asInstanceOf[Byte]
					System.arraycopy(lengthBytes, 0, d, 1, lengthBytes.length)
					(d, lengthBytes.length + 1)
				}
			//リストの要素を、結果配列の中の本体部分にコピーする。
			var copyPos = initialPos
			seq.foreach {
				element => {
					System.arraycopy(element, 0, data, copyPos, element.length)
					copyPos += element.length
				}
			}
			data
		}

		private def encodeSeq(list: Seq[Any]): Array[Byte] = {
			val listOfBytes = list.map(each => encodeElement(each))
			encodeSeqOfByteArrays(listOfBytes)
		}

		@tailrec
		private def encodeElement(elem: Any): Array[Byte] = {
			elem match {
				case bytes: Array[Byte] => encodeItem(bytes)
				case seq: Seq[_] => encodeSeq(seq)
				case v: Value => encodeElement(v.value)
				case Right(v) => encodeElement(v)
				case Left(v) => encodeElement(v)
				case Some(v) => encodeElement(v)
				case _ => encodeItem(elem)
			}
		}

		/**
		 * アイテムをエンコードします。
		 */
		private def encodeItem(value: Any): Array[Byte] = {
			value match {
				case null =>
					//空のアイテムとする。
					Array(OFFSET_SHORT_ITEM.asInstanceOf[Byte])
				case _ =>
					val bytes = toBytes(value)
					if ((bytes.length == 1) && ((bytes(0) & 0xff) < OFFSET_SHORT_ITEM)) {
						bytes
					} else {
						val firstByte = encodeLength(bytes.length, OFFSET_SHORT_ITEM)
						firstByte ++: bytes
					}
			}
		}

		/**
		 * 長さの表現をバイト列にエンコードします。
		 */
		private def encodeLength(length: Int, offset: Int): Array[Byte] = {
			if (length < SIZE_THRESHOLD) {
				Array((length + offset).asInstanceOf[Byte])
			} else {
				val binaryLength =
					if (0xff < length) {
						bytesFromInt(length)
					} else {
						Array(length.asInstanceOf[Byte])
					}
				//渡ってくる offset は、常に短いリストやアイテムの基準点なので、
				//それを長いリストやアイテムに換算するために、SIZE_THRESHOLDを足す。
				val firstByte = (binaryLength.length + offset + SIZE_THRESHOLD - 1).asInstanceOf[Byte]
				firstByte +: binaryLength
			}
		}

		@tailrec
		private def toBytes(input: Any): Array[Byte] = {
			input match {
				case v: Array[Byte] => v
				case v: String => v.getBytes(StandardCharsets.UTF_8)
				case v: Long =>
					if (v == 0L) {
						Array.empty
					} else {
					 	ByteUtils.asUnsignedByteArray(BigInt(v))
					}
				case v: Int =>
					if (v <= 0xff) {
						//１バイト。
						toBytes(v.asInstanceOf[Byte])
					} else if (v <= 0xffff) {
						//２バイト。
						toBytes(v.asInstanceOf[Short])
					} else if (v <= 0xffffff) {
						//３バイト。
						Array(
							(v >>> 16).asInstanceOf[Byte],
							(v >>> 8).asInstanceOf[Byte],
							v.asInstanceOf[Byte]
						)
					} else {
						//４バイト。
						Array(
							(v >>> 24).asInstanceOf[Byte],
							(v >>> 16).asInstanceOf[Byte],
							(v >>> 8).asInstanceOf[Byte],
							v.asInstanceOf[Byte]
						)
					}
				case v: Short =>
					if ((v & 0xFFFF) <= 0xff) {
						//１バイト。
						toBytes(v.asInstanceOf[Byte])
					} else {
						//２バイト。
						Array(
							(v >>> 8).asInstanceOf[Byte],
							v.asInstanceOf[Byte]
						)
					}
				case v: Byte =>
					if (v == 0) {
						Array.empty
					} else {
						Array((v & 0xff).asInstanceOf[Byte])
					}
				case v: Boolean =>
					if (v) {
						toBytes(1.asInstanceOf[Byte])
					} else {
						toBytes(0.asInstanceOf[Byte])
					}
				case v: BigInt =>
					if (v == BIGINT_ZERO) {
						Array.empty
					} else {
						ByteUtils.asUnsignedByteArray(v)
					}
				case _ =>
					throw new RuntimeException("Unsupported type: %s".format(input.getClass + " " + input))
			}
		}
	}

	object Decoder {

		trait DecodedResult {
			def pos: Int
			def isSeq: Boolean
			def bytes: Array[Byte]
			def items: Seq[DecodedResult]

			def result: Any = {
				if (this.isSeq) mapElementsToBytes(items) else bytes
			}

			def asPositiveLong: Long = {
				if (bytes.length == 0) return 0
				BigInt(1, bytes).longValue()
			}

			private def mapElementsToBytes(seq: Seq[DecodedResult]): Seq[AnyRef] = {
				seq.map {
					each => {
						if (!each.isSeq) {
							each.bytes
						} else {
							mapElementsToBytes(each.items)
						}
					}
				}
			}

			def printRecursively(out: PrintStream): Unit = {
				if (this.isSeq) {
					out.print("[")
					this.items.foreach { each =>
						each.printRecursively(out)
					}
					out.print("]")
				} else {
					out.print(Hex.encodeHexString(this.bytes))
					out.print(", ")
				}
			}
		}

		case class DecodedBytes(override val pos: Int, override val bytes: Array[Byte]) extends DecodedResult {
			override val isSeq = false
			override val items = List.empty
		}

		case class DecodedSeq(override val pos: Int, override val items: Seq[DecodedResult]) extends DecodedResult {
			override val isSeq = true
			override val bytes = Array.empty[Byte]
		}

		def decode(data: Array[Byte]): Either[Exception, DecodedResult] = {
			decode(data, 0)
		}

		def decode(data: Array[Byte], pos: Int): Either[Exception, DecodedResult] = {
			import java.util.Arrays._

			if (data.length < 1) {
				return Left(new IllegalArgumentException("Prefix is lacking."))
			}
			val prefix = data(pos) & 0xFF
			if (prefix == OFFSET_SHORT_ITEM) {
				//空データであることが確定。
				Right(DecodedBytes(pos + 1, Array.empty))
			} else if (prefix < OFFSET_SHORT_ITEM) {
				//１バイトデータ。
				Right(DecodedBytes(pos + 1, Array[Byte](data(pos))))
			} else if (prefix <= OFFSET_LONG_ITEM) {//この判定条件は、バグではない。
			//長さがprefixに含まれている。
			val len = prefix - OFFSET_SHORT_ITEM
				Right(DecodedBytes(pos + 1 + len, copyOfRange(data, pos + 1, pos + 1 + len)))
			} else if (prefix < OFFSET_SHORT_LIST) {
				//長さが２重にエンコードされている。
				val lenlen = prefix - OFFSET_LONG_ITEM
				val len = intFromBytes(copyOfRange(data, pos + 1, pos + 1 + lenlen))
				Right(DecodedBytes(pos + 1 + lenlen + len, copyOfRange(data, pos + 1 + lenlen, pos + 1 + lenlen + len)))
			} else if (prefix <= OFFSET_LONG_LIST) {//この判定条件は、バグではない。
			//単純なリスト。
			val len = prefix - OFFSET_SHORT_LIST
				decodeSeq(data, pos + 1, len)
			} else if (prefix < 0xFF) {
				//長さが２重にエンコードされている。
				val lenlen = prefix - OFFSET_LONG_LIST
				val len = intFromBytes(copyOfRange(data, pos + 1, pos + 1 + lenlen))
				decodeSeq(data, pos + lenlen + 1, len)
			} else {
				Left(new IllegalArgumentException("Illegal prefix: %d".format(prefix)))
			}
		}

		private def decodeSeq(data: Array[Byte], pos: Int, len: Int): Either[Exception, DecodedSeq] = {
			decodeListItemsRecursively(data, pos, len, 0, new ArrayBuffer[DecodedResult])
		}

		@tailrec
		private def decodeListItemsRecursively(data: Array[Byte], pos: Int, len: Int, consumed: Int, items: ArrayBuffer[DecodedResult]): Either[Exception, DecodedSeq] = {
			if (len <= consumed) {
				return Right(DecodedSeq(pos, items.toIndexedSeq))
			}
			decode(data, pos) match {
				case Right(item) =>
					items.append(item)
					decodeListItemsRecursively(data, item.pos, len, consumed + (item.pos - pos), items)
				case Left(e) =>
					Left(e)
			}
		}

		private def intFromBytes(b: Array[Byte]): Int = {
			if (b.length == 0) return 0
			BigInt(1, b).intValue()
		}

	}

}
