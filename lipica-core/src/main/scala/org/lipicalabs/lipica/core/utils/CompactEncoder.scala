package org.lipicalabs.lipica.core.utils

import java.io.ByteArrayOutputStream

import org.apache.commons.codec.binary.Hex

/**
 * 十六進文字列を、終端記号付き（オプショナル）のバイト列に変換するための実装オブジェクトです。
 *
 * 終端記号は、trie、Patricia-Tree、Merkle-Patricia-Tree で
 * 接頭辞、接尾辞関係を扱うために必要となるので、
 * その有無を表すフラグを持つこととします。
 *
 * また、例えば "0f1248" という文字列を Seq(15, 18, 72) に単純にエンコードした場合、
 * エンコードされたバイト列を、"0f1248" に戻すべきか "f1248" に戻すべきか
 * わからない、という問題が発生してしまいます。
 * これを解決するために、元のバイト列が偶数か奇数かを表すフラグを持つこととします。
 */
object CompactEncoder {

	val TERMINATOR = 16.toByte

	private val HEX_MAP = Map[Char, Byte](
		'0' -> 0x0,
		'1' -> 0x1,
		'2' -> 0x2,
		'3' -> 0x3,
		'4' -> 0x4,
		'5' -> 0x5,
		'6' -> 0x6,
		'7' -> 0x7,
		'8' -> 0x8,
		'9' -> 0x9,
		'a' -> 0xa,
		'b' -> 0xb,
		'c' -> 0xc,
		'd' -> 0xd,
		'e' -> 0xe,
		'f' -> 0xf
	)

	/**
	 * 「ゼロ以上16未満の値の並び」＋「終端記号 or none」からなるバイト列を、
	 * 半分の長さに圧縮したバイト列に、ヘッダを付けて返します。
	 *
	 * １nibbleを1 byteで贅沢に表し、場合によっては終端器語を付けているバイト列を、
	 * およそ半分に圧縮する、ということ。
	 */
	def packNibbles(aNibbles: Array[Byte]): Array[Byte] = {
		val (terminator, nibbles) =
			if (aNibbles(aNibbles.length - 1) == TERMINATOR) {
				(1, java.util.Arrays.copyOf(aNibbles, aNibbles.length - 1))
			} else {
				(0, aNibbles)
			}

		val oddlen = nibbles.length % 2
		//上位ビットが終端記号の有無、下位ビットが奇数か否か。
		val flag = 2 * terminator + oddlen
		val concatenated =
			if (oddlen != 0) {
				//フラグ分１バイトを足して偶数長にする。
				Array[Byte](flag.toByte) ++ nibbles
			} else {
				//フラグ分＋パディングの合計２バイトを足して偶数長にする。
				Array[Byte](flag.toByte, 0) ++ nibbles
			}
		val buffer = new ByteArrayOutputStream
		var i = 0
		while (i < concatenated.length) {
			//２バイトを１バイトに詰め込む。
			buffer.write(16 * concatenated(i) + concatenated(i + 1))
			i += 2
		}
		buffer.toByteArray
	}

	/**
	 * packNibbles の逆操作を実行して結果を返します。
	 *
	 * バイト列のヘッダを解釈し、バイト列の本体を、
	 * １nibbleを1 byteで贅沢に表し、場合によっては終端記号を付ける形式に変換して返します。
	 */
	def unpackToNibbles(bytes: Array[Byte]): Array[Byte] = {
		val base = binToNibbles(bytes)
		var result = java.util.Arrays.copyOf(base, base.length - 1)
		if (2 <= result(0)) {
			//終端付きである。
			result = result :+ TERMINATOR
		}
		if (result(0) % 2 == 1) {
			//奇数長であった。
			java.util.Arrays.copyOfRange(result, 1, result.length)
		} else {
			//偶数長であった。
			java.util.Arrays.copyOfRange(result, 2, result.length)
		}
	}

	/**
	 * バイト列を、１ニブル（＝４ビット）１文字（＝１バイト）のバイト列に変換し、
	 * 終端記号を付けて返します。
	 * すなわち、バイト数は 2 * bytes.length + 1 に増大します。
	 */
	def binToNibbles(bytes: Array[Byte]): Array[Byte] = {
		binToNibbles(bytes, withTerminator = true)
	}

	/**
	 * バイト列を、１ニブル（＝４ビット）１文字（＝１バイト）のバイト列に変換し、
	 * 終端記号を付けずに返します。
	 * すなわち、バイト数は 2 * bytes.length に増大します。
	 */
	def binToNibblesWithoutTerminator(bytes: Array[Byte]): Array[Byte] = {
		binToNibbles(bytes, withTerminator = false)
	}

	private def binToNibbles(bytes: Array[Byte], withTerminator: Boolean): Array[Byte] = {
		val hexChars = Hex.encodeHex(bytes)
		val result =
			if (withTerminator) {
				new Array[Byte](hexChars.length + 1)
			} else {
				new Array[Byte](hexChars.length)
			}
		(0 until hexChars.length).foreach {
			i => {
				val c = hexChars(i)
				result(i) = HEX_MAP.get(c).get
			}
		}
		if (withTerminator) {
			result(result.length - 1) = TERMINATOR
		}
		result
	}

}
