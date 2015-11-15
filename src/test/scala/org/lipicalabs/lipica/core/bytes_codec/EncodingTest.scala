package org.lipicalabs.lipica.core.bytes_codec

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}
import org.lipicalabs.lipica.core.utils.RBACCodec.Encoder
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class EncodingTest extends Specification {
	sequential

	"encode single Byte" should {
		"run right" in {
			Encoder.encode(0.asInstanceOf[Byte]) sameElements  Array(0x80.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(120.asInstanceOf[Byte]) sameElements Array(0x78.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(127.asInstanceOf[Byte]) sameElements Array(0x7F.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding single Short" should {
		"run right" in {
			RBACCodec.Encoder.encode(0.asInstanceOf[Short]) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(120.asInstanceOf[Short]) sameElements Array(0x78.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(127.asInstanceOf[Short]) sameElements Array(0x7f.asInstanceOf[Byte]) mustEqual true

			RBACCodec.Encoder.encode(30303.asInstanceOf[Short]) sameElements Array[Byte](0x82.asInstanceOf[Byte], 0x76.asInstanceOf[Byte], 0x5f.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(20202.asInstanceOf[Short]) sameElements Array[Byte](0x82.asInstanceOf[Byte], 0x4e.asInstanceOf[Byte], 0xea.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding single Int" should {
		"run right" in {
			RBACCodec.Encoder.encode(0) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(120) sameElements Array(0x78.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(127) sameElements Array(0x7f.asInstanceOf[Byte]) mustEqual true

			RBACCodec.Encoder.encode(128) sameElements Array(0x81.asInstanceOf[Byte], 0x80.asInstanceOf[Byte]) mustEqual true

			RBACCodec.Encoder.encode(30303) sameElements Array(0x82.asInstanceOf[Byte], 0x76.asInstanceOf[Byte], 0x5f.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(20202) sameElements Array(0x82.asInstanceOf[Byte], 0x4e.asInstanceOf[Byte], 0xea.asInstanceOf[Byte]) mustEqual true

			RBACCodec.Encoder.encode(65536) sameElements Array(0x83.asInstanceOf[Byte], 1.asInstanceOf[Byte], 0.asInstanceOf[Byte], 0.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(Int.MinValue) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(Int.MaxValue) sameElements Array(0x84.asInstanceOf[Byte], 0x7f.asInstanceOf[Byte], 0xff.asInstanceOf[Byte], 0xff.asInstanceOf[Byte], 0xff.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding single BigInt" should {
		"run right" in {
			RBACCodec.Encoder.encode(BigInt(0)) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding string" should {
		"run right" in {
			val s = "Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++"
			val expected = Array[Byte](
				0xB8.asInstanceOf[Byte], 0x5A.asInstanceOf[Byte], 0x45.asInstanceOf[Byte], 0x74.asInstanceOf[Byte],
				0x68.asInstanceOf[Byte], 0x65.asInstanceOf[Byte], 0x72.asInstanceOf[Byte], 0x65.asInstanceOf[Byte],
				0x75.asInstanceOf[Byte], 0x6D.asInstanceOf[Byte], 0x28.asInstanceOf[Byte], 0x2B.asInstanceOf[Byte],
				0x2B.asInstanceOf[Byte], 0x29.asInstanceOf[Byte], 0x2F.asInstanceOf[Byte],
				0x5A.asInstanceOf[Byte], 0x65.asInstanceOf[Byte], 0x72.asInstanceOf[Byte], 0x6F.asInstanceOf[Byte],
				0x47.asInstanceOf[Byte], 0x6F.asInstanceOf[Byte], 0x78.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x76.asInstanceOf[Byte], 0x30.asInstanceOf[Byte], 0x2E.asInstanceOf[Byte],
				0x35.asInstanceOf[Byte], 0x2E.asInstanceOf[Byte], 0x30.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x6E.asInstanceOf[Byte], 0x63.asInstanceOf[Byte], 0x75.asInstanceOf[Byte],
				0x72.asInstanceOf[Byte], 0x73.asInstanceOf[Byte], 0x65.asInstanceOf[Byte],
				0x73.asInstanceOf[Byte], 0x2F.asInstanceOf[Byte], 0x4C.asInstanceOf[Byte], 0x69.asInstanceOf[Byte],
				0x6E.asInstanceOf[Byte], 0x75.asInstanceOf[Byte], 0x78.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x67.asInstanceOf[Byte], 0x2B.asInstanceOf[Byte], 0x2B.asInstanceOf[Byte],
				0x45.asInstanceOf[Byte], 0x74.asInstanceOf[Byte], 0x68.asInstanceOf[Byte], 0x65.asInstanceOf[Byte],
				0x72.asInstanceOf[Byte], 0x65.asInstanceOf[Byte],
				0x75.asInstanceOf[Byte], 0x6D.asInstanceOf[Byte], 0x28.asInstanceOf[Byte], 0x2B.asInstanceOf[Byte],
				0x2B.asInstanceOf[Byte], 0x29.asInstanceOf[Byte], 0x2F.asInstanceOf[Byte],
				0x5A.asInstanceOf[Byte], 0x65.asInstanceOf[Byte], 0x72.asInstanceOf[Byte], 0x6F.asInstanceOf[Byte],
				0x47.asInstanceOf[Byte], 0x6F.asInstanceOf[Byte], 0x78.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x76.asInstanceOf[Byte], 0x30.asInstanceOf[Byte], 0x2E.asInstanceOf[Byte],
				0x35.asInstanceOf[Byte], 0x2E.asInstanceOf[Byte], 0x30.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x6E.asInstanceOf[Byte], 0x63.asInstanceOf[Byte], 0x75.asInstanceOf[Byte],
				0x72.asInstanceOf[Byte], 0x73.asInstanceOf[Byte], 0x65.asInstanceOf[Byte],
				0x73.asInstanceOf[Byte], 0x2F.asInstanceOf[Byte], 0x4C.asInstanceOf[Byte], 0x69.asInstanceOf[Byte],
				0x6E.asInstanceOf[Byte], 0x75.asInstanceOf[Byte], 0x78.asInstanceOf[Byte],
				0x2F.asInstanceOf[Byte], 0x67.asInstanceOf[Byte], 0x2B.asInstanceOf[Byte], 0x2b
			)
			RBACCodec.Encoder.encode(s) sameElements expected mustEqual true
		}
	}

	"encoding byte array" should {
		"run right" in {
			val hexString = "ce73660a06626c1b3fda7b18ef7ba3ce17b6bf604f9541d3c6c654b7ae88b239407f659c78f419025d785727ed017b6add21952d7e12007373e321dbc31824ba"
			val bytes = Hex.decodeHex(hexString.toCharArray)
			val expected = "b840" + hexString
			new String(Hex.encodeHex(RBACCodec.Encoder.encode(bytes).toByteArray)).toLowerCase mustEqual expected
		}
	}

	"encoding empty array" should {
		"run right" in {
			RBACCodec.Encoder.encode(List.empty) sameElements Array(0xc0.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding null" should {
		"run right" in {
			RBACCodec.Encoder.encode(null) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
		}
	}

	"encoding small byte array" should {
		"run right" in {
			RBACCodec.Encoder.encode(Array.empty[Byte]) sameElements Array(0x80.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(Array(0x00.asInstanceOf[Byte])) sameElements Array(0x00.asInstanceOf[Byte]) mustEqual true
			RBACCodec.Encoder.encode(Array(0x01.asInstanceOf[Byte])) sameElements Array(0x01.asInstanceOf[Byte]) mustEqual true
		}
	}

	"complex case" should {
		"run right" in {

			val elem1 = RBACCodec.Encoder.encode(Array[Byte](
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
			))
			val elem2 = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(ImmutableBytes.empty)).sha3
			val elem3 = RBACCodec.Encoder.encode(Array[Byte](
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00
			))
			val encoded = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(elem1, elem2, elem3))

			encoded.toHexString mustEqual
				"f856a000000000000000000000000000000000000000000000000000000000000000001dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000"
		}
	}


	"new case 1" should {
		"be right" in {
			val data = Seq(Array[Byte](32.toByte, 99.toByte, 97.toByte, 116.toByte), Array[Byte](100.toByte, 111.toByte, 103.toByte))
			val encoded = RBACCodec.Encoder.encode(data)

			encoded.toHexString mustEqual "c9842063617483646f67"
		}
	}

}
