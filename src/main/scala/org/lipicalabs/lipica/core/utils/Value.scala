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

	def asImmutableBytes: ImmutableBytes

	def asBytes: Array[Byte]

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

	def isImmutableBytes: Boolean

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
					case any => any.toString
				}.mkString("(", ",", ")")
			} else if (value.isBytes) {
				"[" + Hex.encodeHexString(value.asBytes) + "]"
			} else if (value.isImmutableBytes) {
				"[" + value.asImmutableBytes.toHexString + "]"
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
			this.encodedValueOptionRef.set(Option(new EncodedValue(ImmutableBytes(encoded))))
		}
		this.encodedValueOptionRef.get.get
	}

	override def encodedBytes: ImmutableBytes = encode.encodedBytes

	def sha3: ImmutableBytes = encode.sha3

	def hash: ImmutableBytes = sha3

	def asObj: Any = value

	def asSeq: Seq[AnyRef] = this.value.asInstanceOf[Seq[AnyRef]]

	def asImmutableBytes: ImmutableBytes = {
		if (isImmutableBytes) {
			this.value.asInstanceOf[ImmutableBytes]
		} else if (isBytes) {
			ImmutableBytes(this.value.asInstanceOf[Array[Byte]])
		} else if (isString) {
			ImmutableBytes(asString.getBytes(StandardCharsets.UTF_8))
		} else {
			ImmutableBytes.empty
		}
	}

	def asBytes: Array[Byte] = {
		if (isBytes) {
			this.value.asInstanceOf[Array[Byte]]
		} else if (isImmutableBytes) {
			this.value.asInstanceOf[ImmutableBytes].toByteArray
		} else if (isString) {
			asString.getBytes(StandardCharsets.UTF_8)
		} else {
			Array.emptyByteArray
		}
	}

	def asInt: Int = {
		if (isInt) {
			value.asInstanceOf[Int]
		} else if (isBytes) {
			BigInt(1, asBytes).intValue()
		} else if (isImmutableBytes) {
			asImmutableBytes.toPositiveBigInt.intValue()
		} else {
			0
		}
	}

	def asLong: Long = {
		if (isLong) {
			value.asInstanceOf[Long]
		} else if (isInt) {
			value.asInstanceOf[Int].toLong
		} else if (isBytes) {
			BigInt(1, asBytes).longValue()
		} else if (isImmutableBytes) {
			asImmutableBytes.toPositiveBigInt.longValue()
		} else {
			0L
		}
	}

	def asBigInt: BigInt = {
		if (isBigInt) {
			this.value.asInstanceOf[BigInt]
		} else if (isBytes) {
			BigInt(1, asBytes)
		} else if (isImmutableBytes) {
			asImmutableBytes.toPositiveBigInt
		} else if (isLong) {
			BigInt(asLong)
		} else if (isInt) {
			BigInt(asInt)
		} else {
			BigInt(0L)
		}
	}

	def asString: String = {
		if (isString) {
			this.value.asInstanceOf[String]
		} else if (isBytes) {
			new String(asBytes, StandardCharsets.UTF_8)
		} else if (isImmutableBytes) {
			asImmutableBytes.asString(StandardCharsets.UTF_8)
		} else {
			""
		}
	}

	def get(index: Int): Option[Value] = {
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

	def isString: Boolean = this.value.isInstanceOf[String]

	def isInt: Boolean = this.value.isInstanceOf[Int]

	def isLong: Boolean = this.value.isInstanceOf[Long]

	def isBigInt: Boolean = this.value.isInstanceOf[BigInt]

	def isBytes: Boolean = this.value.isInstanceOf[Array[Byte]]

	def isImmutableBytes: Boolean = this.value.isInstanceOf[ImmutableBytes]

	def isHashCode: Boolean = isImmutableBytes && (asImmutableBytes.length == 32)

	def isSeq: Boolean = {
		if (!this.value.isInstanceOf[AnyRef]) return false
		val v = this.value.asInstanceOf[AnyRef]
		(v ne null) && !isString && v.isInstanceOf[Seq[_]]
	}

	def isNull: Boolean = {
		try {
			this.value.asInstanceOf[AnyRef] eq null
		} catch {
			case any: Throwable => false
		}
	}

	def length: Int = {
		if (isSeq) {
			asSeq.size
		} else if (isBytes) {
			asBytes.length
		} else if (isImmutableBytes) {
			asImmutableBytes.length
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
	def value: Any = decode.value

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

	def hash: ImmutableBytes = sha3

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

	def asObj: Any = decode.asObj

	def asSeq: Seq[AnyRef] = decode.asSeq

	def asImmutableBytes: ImmutableBytes = decode.asImmutableBytes

	def asBytes: Array[Byte] = decode.asBytes

	def asInt: Int = decode.asInt

	def asLong: Long = decode.asLong

	def asBigInt: BigInt = decode.asBigInt

	def asString: String = decode.asString

	def get(index: Int): Option[Value] = decode.get(index)

	def isString: Boolean = decode.isString

	def isInt: Boolean = decode.isInt

	def isLong: Boolean = decode.isLong

	def isBigInt: Boolean = decode.isBigInt

	def isBytes: Boolean = decode.isBytes

	def isImmutableBytes: Boolean = decode.isImmutableBytes

	def isHashCode: Boolean = decode.isHashCode

	def isSeq: Boolean = decode.isSeq

	def isNull: Boolean = decode.isNull

	def length: Int = decode.length

	override def toString: String = Value.toString(this)

}
