package org.lipicalabs.lipica.core.utils

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils

import scala.annotation.tailrec

/**
 * オブジェクト、エンコードされたバイト列、ハッシュ値の相互変換を実行し、
 * また一度計算された値を再計算しなくて済むようにキャッシュするための、
 * 値ラッパークラスです。
 */
class Value private(_valueOption: Option[Any], _encodedBytesOption: Option[Array[Byte]]) {

	/**
	 * 値。
	 */
	private val valueOptionRef = new AtomicReference[Option[Any]](None)

	_valueOption match {
		case Some(v) =>
			//値による初期化。
			this.valueOptionRef.set(launderValue(v))
		case _ =>
			//値はない。
	}

	/**
	 * 入れ子になったValueクラスであった場合には、
	 * その中身をたぐって値を取得します。
	 */
	@tailrec
	private def launderValue(any: Any): Option[Any] = {
		any match {
			case null =>
				Some("")
			case another: Value =>
				launderValue(another.value)
			case _ =>
				Option(any)
		}
	}

	/**
	 * ラップ対象の値を返します。
	 */
	def value: Any = decode

	/**
	 * エンコードされたバイト列。
	 */
	private val encodedBytesOptionRef = new AtomicReference(_encodedBytesOption)

	/**
	 * SHA3ダイジェスト値。
	 */
	private val sha3OptionRef = new AtomicReference[Option[Array[Byte]]](None)

	def sha3: Array[Byte] = hash

	def decode: Any = {
		if (this.valueOptionRef.get.isEmpty) {
			RBACCodec.Decoder.decode(this.encode) match {
				case Right(result) =>
					this.valueOptionRef.set(Option(result.result))
				case Left(e) => ()
			}
		}
		this.valueOptionRef.get.orNull
	}

	def encode: Array[Byte] = {
		if (this.encodedBytesOptionRef.get.isEmpty) {
			val encoded = RBACCodec.Encoder.encode(value)
			this.encodedBytesOptionRef.set(Some(encoded))
		}
		this.encodedBytesOptionRef.get.get
	}

	def hash: Array[Byte] = {
		if (this.sha3OptionRef.get.isEmpty) {
			val encoded = encode
			val sha3 = DigestUtils.sha3(encoded)
			this.sha3OptionRef.set(Some(sha3))
		}
		this.sha3OptionRef.get.get
	}

	def asObj: Any = {
		decode
	}

	def asSeq: Seq[AnyRef] = {
		decode
		this.value.asInstanceOf[Seq[AnyRef]]
	}

	def asBytes: Array[Byte] = {
		decode
		if (isBytes) {
			this.value.asInstanceOf[Array[Byte]]
		} else if (isString) {
			asString.getBytes(StandardCharsets.UTF_8)
		} else {
			Array.empty[Byte]
		}
	}

	def asInt: Int = {
		decode
		if (isInt) {
			value.asInstanceOf[Int]
		} else if (isBytes) {
			BigInt(1, asBytes).intValue()
		} else {
			0
		}
	}

	def asLong: Long = {
		decode
		if (isLong) {
			value.asInstanceOf[Long]
		} else if (isBytes) {
			BigInt(1, asBytes).longValue()
		} else {
			0L
		}
	}

	def asBigInt: BigInt = {
		decode
		if (isBigInt) {
			this.value.asInstanceOf[BigInt]
		} else if (isBytes) {
			BigInt(1, asBytes)
		} else {
			BigInt(0L)
		}
	}

	def asString: String = {
		decode
		if (isString) {
			this.value.asInstanceOf[String]
		} else if (isBytes) {
			new String(asBytes, StandardCharsets.UTF_8)
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

	def isString: Boolean = {
		decode
		this.value.isInstanceOf[String]
	}

	def isInt: Boolean = {
		decode
		this.value.isInstanceOf[Int]
	}

	def isLong: Boolean = {
		decode
		this.value.isInstanceOf[Long]
	}

	def isBigInt: Boolean = {
		decode
		this.value.isInstanceOf[BigInt]
	}

	def isBytes: Boolean = {
		decode
		this.value.isInstanceOf[Array[Byte]]
	}

	def isSeq: Boolean = {
		decode
		if (!this.value.isInstanceOf[AnyRef]) return false
		val v = this.value.asInstanceOf[AnyRef]
		(v ne null) && !isString && v.isInstanceOf[Seq[_]]
	}

	def isNull: Boolean = {
		decode
		try {
			this.value.asInstanceOf[AnyRef] eq null
		} catch {
			case any: Throwable => false
		}
	}

	def length: Int = {
		decode
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

	override def toString: String = {
		val result =
			if (isSeq) {
				val seq = asSeq
				seq.map {
					case null => ""
					case s: String => s
					case bytes: Array[Byte] => "[" + Hex.encodeHexString(bytes) + "]"
					case any => any.toString
				}.mkString("(", ",", ")")
			} else if (isBytes) {
				"[" + Hex.encodeHexString(asBytes) + "]"
			} else if (isString) {
				asString
			} else {
				asObj.getClass
			}
		"Value(%s)".format(result)
	}

}

object Value {

	def fromObject(obj: Any): Value = {
		obj match {
			case v: Value => v
			case _ => new Value(Some(obj), None)
		}
	}

	def fromEncodedBytes(encodedBytes: Array[Byte]): Value = {
		new Value(None, Option(encodedBytes))
	}

	val empty = fromObject(Array.emptyByteArray)

}
