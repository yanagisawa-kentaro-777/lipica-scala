package org.lipicalabs.lipica.core.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CompactEncoderTest extends Specification {
sequential

	private val T = 16.toByte

	"pack nibbles (odd not terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](1, 2, 3, 4, 5))
			val expected = Array[Byte](0x11, 0x23, 0x45)
			(CompactEncoder.packNibbles(test) sameElements expected) mustEqual true
		}
	}

	"pack nibbles (even not terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0, 1, 2, 3, 4, 5))
			val expected = Array[Byte](0x00, 0x01, 0x23, 0x45)
			(CompactEncoder.packNibbles(test) sameElements expected) mustEqual true
		}
	}

	"pack nibbles (even terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0, 15, 1, 12, 11, 8, T))
			val expected = Array[Byte](0x20, 0x0f, 0x1c, 0xb8.asInstanceOf[Byte])
			(CompactEncoder.packNibbles(test) sameElements expected) mustEqual true
		}
	}

	"pack nibbles (odd terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](15, 1, 12, 11, 8, T))
			val expected = Array[Byte](0x3f, 0x1c, 0xb8.asInstanceOf[Byte])
			(CompactEncoder.packNibbles(test) sameElements expected) mustEqual true
		}
	}

	"unpack to nibbles (odd not terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0x11, 0x23, 0x45))
			val expected = Array[Byte](1, 2, 3, 4, 5)
			(CompactEncoder.unpackToNibbles(test) sameElements expected) mustEqual true
		}
	}

	"unpack to nibbles (even not terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0x00, 0x01, 0x23, 0x45))
			val expected = Array[Byte](0, 1, 2, 3, 4, 5)
			(CompactEncoder.unpackToNibbles(test) sameElements expected) mustEqual true
		}
	}

	"unpack to nibbles (even terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0x20, 0x0f, 0x1c, 0xb8.toByte))
			val expected = Array[Byte](0, 15, 1, 12, 11, 8, T)
			(CompactEncoder.unpackToNibbles(test) sameElements expected) mustEqual true
		}
	}

	"unpack to nibbles (odd terminated)" should {
		"be right" in {
			val test = ImmutableBytes(Array[Byte](0x3f, 0x1c, 0xb8.toByte))
			val expected = Array[Byte](15, 1, 12, 11, 8, T)
			(CompactEncoder.unpackToNibbles(test) sameElements expected) mustEqual true
		}
	}

	"bin to nibbles 1" should {
		"be right" in {
			val test = ImmutableBytes("stallion".getBytes)
			val expected = Array[Byte](7, 3, 7, 4, 6, 1, 6, 12, 6, 12, 6, 9, 6, 15, 6, 14, T)

			val converted = CompactEncoder.binToNibbles(test)
			(converted sameElements expected) mustEqual true
		}
	}

	"bin to nibbles 2" should {
		"be right" in {
			val test = ImmutableBytes("verb".getBytes)
			val expected = Array[Byte](7, 6, 6, 5, 7, 2, 6, 2, T)

			val converted = CompactEncoder.binToNibbles(test)
			(converted sameElements expected) mustEqual true
		}
	}

	"bin to nibbles 3" should {
		"be right" in {
			val test = ImmutableBytes("puppy".getBytes)
			val expected = Array[Byte](7, 0, 7, 5, 7, 0, 7, 0, 7, 9, T)

			val converted = CompactEncoder.binToNibbles(test)
			(converted sameElements expected) mustEqual true
		}
	}

}
