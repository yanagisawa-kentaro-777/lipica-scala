package org.lipicalabs.lipica.core.utils

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.codec.binary.Hex

import scala.annotation.tailrec

/**
 * オブジェクト、エンコードされたバイト列、ハッシュ値の相互変換を実行し、
 * また一度計算された値を再計算しなくて済むようにキャッシュするための、
 * 値ラッパークラスです。
 */
trait Value {
	/**
	 * ラップ対象の値を返します。
	 */
	def value: Any

	def encodedBytes: ImmutableBytes

	def hash: ImmutableBytes

	def asObj: Any

	def asSeq: Seq[AnyRef]

	def asBytes: ImmutableBytes

	def asInt: Int

	def asLong: Long

	def asBigInt: BigInt

	def asString: String

	def get(index: Int): Option[Value]

	def isString: Boolean

	def isInt: Boolean

	def isLong: Boolean

	def isBigInt: Boolean

	def isBytes: Boolean

	def isHashCode: Boolean

	def isSeq: Boolean

	def isNull: Boolean

	def length: Int

}

object Value {

	val empty: PlainValue = fromObject(ImmutableBytes.empty).asInstanceOf[PlainValue]

	def fromObject(obj: Any): Value = {
		obj match {
			case v: Value => v
			case _ => new PlainValue(obj)
		}
	}

	def fromEncodedBytes(encodedBytes: ImmutableBytes): Value = {
		new EncodedValue(encodedBytes)
	}

	def toString(value: Value): String = {
		val result =
			if (value.isSeq) {
				val seq = value.asSeq
				seq.map {
					case null => ""
					case s: String => s
					case bytes: Array[Byte] => "[" + Hex.encodeHexString(bytes) + "]"
					case bytes: ImmutableBytes => "[" + bytes.toHexString + "]"
					case any => any.toString
				}.mkString("(", ",", ")")
			} else if (value.isBytes) {
				"[" + value.asBytes.toHexString + "]"
			} else if (value.isString) {
				value.asString
			} else {
				value.asObj.getClass
			}
		"Value(%s)".format(result)
	}
}

class PlainValue private[utils](_value: Any) extends Value {

	/**
	 * 値。
	 */
	override val value: Any = launderValue(_value)

	/**
	 * 入れ子になったValueクラスであった場合には、
	 * その中身をたぐって値を取得します。
	 */
	@tailrec
	private def launderValue(any: Any): Any = {
		any match {
			case null => ""
			case another: Value => launderValue(another.value)
			case _ => any
		}
	}

	/**
	 * エンコードされたバイト列。
	 */
	private val encodedValueOptionRef: AtomicReference[Option[EncodedValue]] = new AtomicReference(None)

	private def encode: EncodedValue = {
		if (this.encodedValueOptionRef.get.isEmpty) {
			val encoded = RBACCodec.Encoder.encode(value)
			this.encodedValueOptionRef.set(Option(new EncodedValue(encoded)))
		}
		this.encodedValueOptionRef.get.get
	}

	override def encodedBytes: ImmutableBytes = encode.encodedBytes

	def sha3: ImmutableBytes = encode.sha3

	override def hash: ImmutableBytes = sha3

	override def asObj: Any = value

	override def asSeq: Seq[AnyRef] = this.value.asInstanceOf[Seq[AnyRef]]

	override def asBytes: ImmutableBytes = {
		this.value match {
			case v: Array[Byte] => ImmutableBytes(this.value.asInstanceOf[Array[Byte]])
			case v: ImmutableBytes => v
			case v: String => ImmutableBytes(v.getBytes(StandardCharsets.UTF_8))
			case _ => ImmutableBytes.empty
		}
	}

	override def asInt: Int = {
		if (isInt) {
			value.asInstanceOf[Int]
		} else if (isBytes) {
			asBytes.toPositiveBigInt.intValue()
		} else {
			0
		}
	}

	override def asLong: Long = {
		if (isLong) {
			value.asInstanceOf[Long]
		} else if (isInt) {
			value.asInstanceOf[Int].toLong
		} else if (isBytes) {
			asBytes.toPositiveBigInt.longValue()
		} else {
			0L
		}
	}

	override def asBigInt: BigInt = {
		if (isBigInt) {
			this.value.asInstanceOf[BigInt]
		} else if (isBytes) {
			asBytes.toPositiveBigInt
		} else if (isLong) {
			BigInt(asLong)
		} else if (isInt) {
			BigInt(asInt)
		} else {
			BigInt(0L)
		}
	}

	override def asString: String = {
		if (isString) {
			this.value.asInstanceOf[String]
		} else if (isBytes) {
			asBytes.asString(StandardCharsets.UTF_8)
		} else {
			""
		}
	}

	override def get(index: Int): Option[Value] = {
		if (isSeq) {
			try {
				Some(Value.fromObject(asSeq(index)))
			} catch {
				case any: Throwable => None
			}
		} else {
			None
		}
	}

	override def isString: Boolean = this.value.isInstanceOf[String]

	override def isInt: Boolean = this.value.isInstanceOf[Int]

	override def isLong: Boolean = this.value.isInstanceOf[Long]

	override def isBigInt: Boolean = this.value.isInstanceOf[BigInt]

	override def isBytes: Boolean = this.value.isInstanceOf[Array[Byte]] || this.value.isInstanceOf[ImmutableBytes]

	override def isHashCode: Boolean = isBytes && (asBytes.length == 32)

	override def isSeq: Boolean = {
		if (!this.value.isInstanceOf[AnyRef]) return false
		val v = this.value.asInstanceOf[AnyRef]
		(v ne null) && !isString && v.isInstanceOf[Seq[_]]
	}

	override def isNull: Boolean = {
		try {
			this.value.asInstanceOf[AnyRef] eq null
		} catch {
			case any: Throwable => false
		}
	}

	override def length: Int = {
		if (isSeq) {
			asSeq.size
		} else if (isBytes) {
			asBytes.length
		} else if (isString) {
			asString.length
		} else {
			0
		}
	}

	override def toString: String = Value.toString(this)

}


class EncodedValue private[utils](override val encodedBytes: ImmutableBytes) extends Value {

	/**
	 * 値。
	 */
	private val plainValueRef = new AtomicReference[Option[PlainValue]](None)

	/**
	 * ラップ対象の値を返します。
	 */
	override def value: Any = decode.value

	/**
	 * SHA3ダイジェスト値。
	 */
	private val sha3OptionRef = new AtomicReference[Option[ImmutableBytes]](None)

	def sha3: ImmutableBytes = {
		if (this.sha3OptionRef.get.isEmpty) {
			val sha3 = encodedBytes.sha3
			this.sha3OptionRef.set(Some(sha3))
		}
		this.sha3OptionRef.get.get
	}

	override def hash: ImmutableBytes = sha3

	def decode: PlainValue = {
		if (this.plainValueRef.get.isEmpty) {
			RBACCodec.Decoder.decode(this.encodedBytes) match {
				case Right(result) =>
					this.plainValueRef.set(Option(new PlainValue(result.result)))
				case Left(e) => ()
			}
		}
		this.plainValueRef.get.getOrElse(Value.empty)
	}

	override def asObj: Any = decode.asObj

	override def asSeq: Seq[AnyRef] = decode.asSeq

	override def asBytes: ImmutableBytes = decode.asBytes

	override def asInt: Int = decode.asInt

	override def asLong: Long = decode.asLong

	override def asBigInt: BigInt = decode.asBigInt

	override def asString: String = decode.asString

	override def get(index: Int): Option[Value] = decode.get(index)

	override def isString: Boolean = decode.isString

	override def isInt: Boolean = decode.isInt

	override def isLong: Boolean = decode.isLong

	override def isBigInt: Boolean = decode.isBigInt

	override def isBytes: Boolean = decode.isBytes

	override def isHashCode: Boolean = decode.isHashCode

	override def isSeq: Boolean = decode.isSeq

	override def isNull: Boolean = decode.isNull

	override def length: Int = decode.length

	override def toString: String = Value.toString(this)

}
