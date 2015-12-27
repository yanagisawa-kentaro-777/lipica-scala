package org.lipicalabs.lipica.core.utils

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.codec.binary.Hex

import scala.annotation.tailrec

/**
 * オブジェクト、エンコードされたバイト列、ハッシュ値の相互変換を実行し、
 * また一度計算された値を再計算しなくて済むようにキャッシュするための、
 * 値ラッパークラスです。
 *
 * 静的な型付という意味では、かなり甘い要素を導入するクラスです。
 */
trait Value {
	/**
	 * ラップ対象の値を返します。
	 */
	def value: Any

	def encodedBytes: ImmutableBytes

	def decode: Value

	def hash: ImmutableBytes

	def asSeq: Seq[AnyRef]

	def asBytes: ImmutableBytes

	def get(index: Int): Option[Value]

	def isBytes: Boolean

	def isHashCode: Boolean

	def isSeq: Boolean

	def isNull: Boolean

	def length: Int

	override def toString: String = Value.toString(this)

	override def hashCode: Int = toString.hashCode

	override def equals(o: Any): Boolean = {
		try {
			this.toString == o.asInstanceOf[Value].toString
		} catch {
			case any: Throwable => false
		}
	}

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
			} else {
				value.value.getClass
			}
		"Value(%s)".format(result)
	}
}

class PlainValue private[utils](_value: Any) extends Value {

	/**
	 * 値。
	 */
	override val value: Any = launderValue(_value)

	override val decode = this

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

	override def hash: ImmutableBytes = encode.hash

	override def asSeq: Seq[AnyRef] = this.value.asInstanceOf[Seq[AnyRef]]

	override def asBytes: ImmutableBytes = {
		this.value match {
			case v: Array[Byte] => ImmutableBytes(this.value.asInstanceOf[Array[Byte]])
			case v: ImmutableBytes => v
			case v: String => ImmutableBytes(v.getBytes(StandardCharsets.UTF_8))
			case _ => ImmutableBytes.empty
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

	override def isBytes: Boolean = {
		this.value.isInstanceOf[Array[Byte]] || this.value.isInstanceOf[ImmutableBytes]
	}

	override def isHashCode: Boolean = isBytes && (asBytes.length == 32)

	override def isSeq: Boolean = {
		try {
			val v = this.value.asInstanceOf[AnyRef]
			(v ne null) && v.isInstanceOf[Seq[_]]
		} catch {
			case any: Throwable => false
		}
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
	 * ダイジェスト値。
	 */
	private val hashOptionRef = new AtomicReference[Option[ImmutableBytes]](None)

	override def hash: ImmutableBytes = {
		if (this.hashOptionRef.get.isEmpty) {
			val digest = encodedBytes.digest256
			this.hashOptionRef.set(Some(digest))
		}
		this.hashOptionRef.get.get
	}

	override def decode: PlainValue = {
		if (this.plainValueRef.get.isEmpty) {
			RBACCodec.Decoder.decode(this.encodedBytes) match {
				case Right(result) =>
					this.plainValueRef.set(Option(new PlainValue(result.result)))
				case Left(e) => ()
			}
		}
		this.plainValueRef.get.getOrElse(Value.empty)
	}

	override def asSeq: Seq[AnyRef] = decode.asSeq

	override def asBytes: ImmutableBytes = decode.asBytes

	override def get(index: Int): Option[Value] = decode.get(index)

	override def isBytes: Boolean = decode.isBytes

	override def isHashCode: Boolean = decode.isHashCode

	override def isSeq: Boolean = decode.isSeq

	override def isNull: Boolean = decode.isNull

	override def length: Int = decode.length

	override def toString: String = Value.toString(this)

}
