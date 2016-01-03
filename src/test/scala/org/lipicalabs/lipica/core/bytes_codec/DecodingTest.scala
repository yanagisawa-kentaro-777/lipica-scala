package org.lipicalabs.lipica.core.bytes_codec

import java.io.{ByteArrayOutputStream, PrintStream}
import java.nio.charset.StandardCharsets

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.{Decoder, Encoder}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.{DecodedSeq, DecodedBytes}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class DecodingTest extends Specification {
	sequential

	private val NUM_RANDOM_CASES = 500

	"empty string" should {
		"run right" in {
			val test = ""
			val expected = "80"
			val encodeResult = Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.asString(StandardCharsets.UTF_8) mustEqual test
		}
	}

	"single char" should {
		"run right" in {
			val test = "d"
			val expected = "64"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			new String(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray, StandardCharsets.UTF_8) mustEqual test
		}
	}

	"short string" should {
		"run right" in {
			val test = "dog"
			val expected = "83646f67"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			new String(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray, StandardCharsets.UTF_8) mustEqual test
		}
	}

	"long string" should {
		"run right" in {
			val test = "Lorem ipsum dolor sit amet, consectetur adipisicing elit"
			val expected = "b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			new String(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray, StandardCharsets.UTF_8) mustEqual test
		}
	}

	"zero" should {
		"run right" in {
			val test = 0
			val expected = "80"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.isEmpty mustEqual true
		}
	}

	"small int" should {
		"run right" in {
			val test = 15
			val expected = "0f"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray) mustEqual test
		}
	}

	"medium int (1000)" should {
		"run right" in {
			val test = 1000
			val expected = "8203e8"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray) mustEqual test
		}
	}

	"medium int (1024)" should {
		"run right" in {
			val test = 1024
			val expected = "820400"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray) mustEqual test
		}
	}

	"big int" should {
		"run right" in {
			val test = BigInt("100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 16)
			val expected = "a0100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			BigInt(1, decodeResult.right.get.asInstanceOf[DecodedBytes].bytes.toByteArray) mustEqual test
		}
	}

	"empty list" should {
		"run right" in {
			val test = Seq.empty
			val expected = "c0"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.isEmpty mustEqual true
		}
	}

	"string list" should {
		"run right" in {
			val test = Seq("dog", "god", "cat")
			val expected = "cc83646f6783676f6483636174"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.size mustEqual 3
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.bytes.toByteArray) mustEqual "dog"
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).bytes.toByteArray) mustEqual "god"
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).bytes.toByteArray) mustEqual "cat"
		}
	}

	"multi list (1)" should {
		"run right" in {
			//in: [ 1, ["cat"], "dog", [ 2 ] ],
			//out: "cc01c48363617483646f67c102"

			val test = Seq(1, Seq("cat"), "dog", Seq(2))
			val expected = "cc01c48363617483646f67c102"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.size mustEqual 4

			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.bytes.toByteArray) mustEqual 1
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).items.head.bytes.toByteArray) mustEqual "cat"
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).bytes.toByteArray) mustEqual "dog"
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedSeq].items(3).items.head.bytes.toByteArray) mustEqual 2
		}
	}

	"multi list (2)" should {
		"run right" in {
			//in: [ [ "cat", "dog" ], [ 1, 2 ], [] ],
			//out: "cdc88363617483646f67c20102c0"

			val test = Seq(Seq("cat", "dog"), Seq(1, 2), Seq.empty)
			val expected = "cdc88363617483646f67c20102c0"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.size mustEqual 3

			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.items.head.bytes.toByteArray) mustEqual "cat"
			new String(decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.items(1).bytes.toByteArray) mustEqual "dog"
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).items.head.bytes.toByteArray) mustEqual 1
			intFromBytes(decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).items(1).bytes.toByteArray) mustEqual 2
			decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).isSeq mustEqual true
			decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).items.isEmpty mustEqual true
		}
	}

	"empty lists" should {
		"run right" in {
			//list: [ [], [[]], [ [], [[]] ] ]
			//out: "c7c0c1c0c3c0c1c0"

			val test = Seq(Seq.empty, Seq(Seq.empty), Seq(Seq.empty, Seq(Seq.empty)))
			val expected = "c7c0c1c0c3c0c1c0"
			val encodeResult = RBACCodec.Encoder.encode(test)
			encodeResult.toHexString mustEqual expected

			val decodeResult = RBACCodec.Decoder.decode(encodeResult)
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.size mustEqual 3

			decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.isSeq mustEqual true
			decodeResult.right.get.asInstanceOf[DecodedSeq].items.head.items.isEmpty mustEqual true

			decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).isSeq mustEqual true
			decodeResult.right.get.asInstanceOf[DecodedSeq].items(1).items.size mustEqual 1

			decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).isSeq mustEqual true
			decodeResult.right.get.asInstanceOf[DecodedSeq].items(2).items.size mustEqual 2
		}
	}

	"byte borders" should {
		"be right" in {
			(0 to 0xff).foreach {
				i => {
					val eachByte = (i & 0xFF).toByte
					val encoded = RBACCodec.Encoder.encode(eachByte)
					val decoded = RBACCodec.Decoder.decode(encoded)
					val rebuilt = (decoded.right.get.asPositiveLong & 0xff).toByte
					eachByte mustEqual rebuilt
				}
			}
			ok
		}
	}

	def encodeAndRebuildInt(value: Int): Int = {
		val encoded = RBACCodec.Encoder.encode(value)
		(RBACCodec.Decoder.decode(encoded).right.get.asPositiveLong & 0xFFFFFFFF).toInt
	}

	"int borders" should {
		"be right" in {
			//負数には非対応。（独自にバイト列にエンコードせよ。）
			0 mustEqual encodeAndRebuildInt(0)
			255 mustEqual encodeAndRebuildInt(255)
			256 mustEqual encodeAndRebuildInt(256)
			257 mustEqual encodeAndRebuildInt(257)

			32767 mustEqual encodeAndRebuildInt(32767)
			32768 mustEqual encodeAndRebuildInt(32768)
			32769 mustEqual encodeAndRebuildInt(32769)

			(0xFFFFFF - 1) mustEqual encodeAndRebuildInt(0xFFFFFF - 1)
			0xFFFFFF mustEqual encodeAndRebuildInt(0xFFFFFF)
			(0xFFFFFF + 1) mustEqual encodeAndRebuildInt(0xFFFFFF + 1)

			(Int.MaxValue - 1) mustEqual encodeAndRebuildInt(Int.MaxValue - 1)
			Int.MaxValue mustEqual encodeAndRebuildInt(Int.MaxValue)
		}
	}

	def encodeAndRebuildLong(value: Long): Long = {
		val encoded = RBACCodec.Encoder.encode(value)
		RBACCodec.Decoder.decode(encoded).right.get.asPositiveLong
	}

	"long borders" should {
		"be right" in {
			//負数には非対応。（独自にバイト列にエンコードせよ。）
			0L mustEqual encodeAndRebuildLong(0L)
			255L mustEqual encodeAndRebuildLong(255L)
			256L mustEqual encodeAndRebuildLong(256L)
			257L mustEqual encodeAndRebuildLong(257L)

			32767L mustEqual encodeAndRebuildLong(32767L)
			32768L mustEqual encodeAndRebuildLong(32768L)
			32769L mustEqual encodeAndRebuildLong(32769L)

			(0xFFFFFFL - 1L) mustEqual encodeAndRebuildLong(0xFFFFFFL - 1L)
			0xFFFFFFL mustEqual encodeAndRebuildLong(0xFFFFFFL)
			(0xFFFFFFL + 1L) mustEqual encodeAndRebuildLong(0xFFFFFFL + 1L)

			(0xFFFFFFFFL - 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFL - 1L)
			0xFFFFFFFFL mustEqual encodeAndRebuildLong(0xFFFFFFFFL)
			(0xFFFFFFFFL + 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFL + 1L)

			(0xFFFFFFFFFFL - 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFL - 1L)
			0xFFFFFFFFFFL mustEqual encodeAndRebuildLong(0xFFFFFFFFFFL)
			(0xFFFFFFFFFFL + 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFL + 1L)

			(0xFFFFFFFFFFFFL - 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFL - 1L)
			0xFFFFFFFFFFFFL mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFL)
			(0xFFFFFFFFFFFFL + 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFL + 1L)

			(0xFFFFFFFFFFFFFFL - 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFFFL - 1L)
			0xFFFFFFFFFFFFFFL mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFFFL)
			(0xFFFFFFFFFFFFFFL + 1L) mustEqual encodeAndRebuildLong(0xFFFFFFFFFFFFFFL + 1L)

			(Long.MaxValue - 1L) mustEqual encodeAndRebuildLong(Long.MaxValue - 1L)
			Long.MaxValue mustEqual encodeAndRebuildLong(Long.MaxValue)
		}
	}

	private def generateByteArray(len: Int): Array[Byte] = {
		val bytes = new Array[Byte](len)
		bytes.indices.foreach {
			idx => {
				bytes(idx) = (idx % 256).toByte
			}
		}
		bytes
	}

	def encodeAndRebuildByteArray(bytes: Array[Byte]): ImmutableBytes = {
		val encoded = RBACCodec.Encoder.encode(bytes)
		//println("%s".format(Hex.encodeHexString(encoded)))
		RBACCodec.Decoder.decode(encoded).right.get.bytes
	}

	"various bytes" should {
		"be right" in {
			(0 until 1000).foreach {
				len => {
					val bytes = generateByteArray(len)
					val rebuilt = encodeAndRebuildByteArray(bytes)
					val matches = (bytes sameElements rebuilt.toByteArray) || ((bytes.length == 1) && (bytes(0) == 0) && rebuilt.isEmpty)
					if (!matches) {
						println("[%,d] %s != %s".format(len, Hex.encodeHexString(bytes), rebuilt.toHexString))
					}
					matches mustEqual true
				}
			}
			ok
		}
	}

	private def generateSeq(len: Int): Seq[Array[Byte]] = {
		(0 until len).map(_ => Array.emptyByteArray).toSeq
	}

	def encodeAndRebuildSeq(seq: Seq[Array[Byte]]): Seq[ImmutableBytes] = {
		val encoded = RBACCodec.Encoder.encode(seq)
		val decoded = RBACCodec.Decoder.decode(encoded).right.get
		decoded.bytes mustEqual encoded
		decoded.result.asInstanceOf[Seq[ImmutableBytes]]
	}

	"various seq" should {
		"be right" in {
			(0 until NUM_RANDOM_CASES).foreach {
				len => {
					val seq = generateSeq(len)
					val rebuilt = encodeAndRebuildSeq(seq)
					seq.map(Hex.encodeHexString) mustEqual rebuilt.map(_.toHexString)
				}
			}
			ok
		}
	}

	"additional case" should {
		"be right" in {
			val data = Hex.decodeHex("f780d3872064636c6f74688a756e6275726e61626c6580808080808080808080808080d387206b76696c6c658a756e646572706c616e7480".toCharArray)
			val result = RBACCodec.Decoder.decode(data).right.get
			result.isSeq mustEqual true
			result.items.size mustEqual 17
			result.bytes mustEqual ImmutableBytes(data)
		}
	}

	"list printing" should {
		"be right" in {
			val s = "F86E12F86B80881BC16D674EC8000094CD2A3D9F938E13CD947EC05ABC7FE734DF8DD8268609184E72A00064801BA0C52C114D4F5A3BA904A9B3036E5E118FE0DBB987FE3955DA20F2CD8F6C21AB9CA06BA4C2874299A55AD947DBC98A25EE895AABF6B625C26C435E84BFD70EDF2F69"
			val data = Hex.decodeHex(s.toCharArray)

			val decoded = RBACCodec.Decoder.decode(data).right.get
			val out = new ByteArrayOutputStream
			val printStream = new PrintStream(out)
			decoded.printRecursively(printStream)
			printStream.flush()
			val result = out.toString(StandardCharsets.UTF_8.name)

			val answer = "[12, [, 1bc16d674ec80000, cd2a3d9f938e13cd947ec05abc7fe734df8dd826, 09184e72a000, 64, , 1b, c52c114d4f5a3ba904a9b3036e5e118fe0dbb987fe3955da20f2cd8f6c21ab9c, 6ba4c2874299a55ad947dbc98a25ee895aabf6b625c26c435e84bfd70edf2f69, ]]"
			result mustEqual answer
		}
	}

	private def intFromBytes(b: Array[Byte]): Int = {
		if (b.length == 0) return 0
		BigInt(1, b).intValue()
	}
}
