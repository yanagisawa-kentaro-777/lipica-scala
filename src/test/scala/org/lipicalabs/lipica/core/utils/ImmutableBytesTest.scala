package org.lipicalabs.lipica.core.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ImmutableBytesTest extends Specification {
sequential

	"elements (1)" should {
		"be right" in {
			val test1 = ImmutableBytes(Array[Byte](1, 2, 3, 4, 5))
			test1.length mustEqual 5
			test1.size mustEqual 5
			test1.isEmpty mustEqual false
			test1.nonEmpty mustEqual true

			test1.indices.foreach {i => {
				test1(i) mustEqual i + 1
			}}
			test1.toString mustEqual "0102030405"
			(test1 equals test1) mustEqual true
			(test1 compareTo test1) mustEqual 0

			val test2 = ImmutableBytes(Array[Byte](1, 2, 3, 4, 5))
			test1 mustEqual test2
			(test1 compareTo test2) mustEqual 0

			val test3 = ImmutableBytes(Array[Byte](1, 2, 3, 4, 5, 6))
			test3.length mustEqual 6
			test3.toString mustEqual "010203040506"
			test1 mustNotEqual test3
			(test1 compareTo test3) mustEqual -1
			(test3 compareTo test1) mustEqual 1

			val byteArray = test3.toByteArray
			byteArray.length mustEqual 6
			byteArray sameElements Array[Byte](1, 2, 3, 4, 5, 6) mustEqual true

			val dest = new Array[Byte](7)
			test3.copyTo(0, dest, 0, test3.length)
			dest sameElements Array[Byte](1, 2, 3, 4, 5, 6, 0) mustEqual true

			test3.firstIndex(_ == 3) mustEqual 2
			test3.firstIndex(_ == 100) mustEqual -1
			test3.count(_ == 2) mustEqual 1
			test3.count(_ == 100) mustEqual 0

			test3.copyOfRange(1, 2) mustEqual ImmutableBytes(Array[Byte](2))

			val test4 = ImmutableBytes(Array[Byte](0xff.toByte))
			(test4.toPositiveBigInt == BigInt(255)) mustEqual true
			test4.toSignedBigInt mustEqual BigInt(-1)
			test4.toSignedBigInteger.toString mustEqual "-1"
		}
	}

	"elements (2)" should {
		"be right" in {
			val test = ImmutableBytes(null.asInstanceOf[Array[Byte]])
			test.length mustEqual 0
			test.isEmpty mustEqual true
			test.nonEmpty mustEqual false
			test.toString mustEqual ""
			(test equals test) mustEqual true
			(test compareTo test) mustEqual 0
		}
	}

	"elements (3)" should {
		"be right" in {
			val test = ImmutableBytes((0 until 256).map(_.toByte).toArray)
			test.length mustEqual 256
			test.toString.length mustEqual 512

			test.indices.foreach {
				i => test.asPositiveInt(i) mustEqual i
			}

			(test equals test) mustEqual true
			(test compareTo test) mustEqual 0
		}
	}

	"elements (4)" should {
		"be right" in {
			val test = ImmutableBytes.create(17)
			test.length mustEqual 17
			test.toString.length mustEqual 34

			test.indices.foreach {
				i => test(i) mustEqual 0
			}
			test mustEqual test
		}
	}

	"concatenation" should {
		"be right" in {
			val bytes1 = ImmutableBytes(Array[Byte](0, 1, 2, 3))
			val bytes2 = ImmutableBytes(Array[Byte](4, 5, 6, 7))

			(bytes1 ++ bytes2) mustEqual ImmutableBytes(Array[Byte](0, 1, 2, 3, 4, 5, 6, 7))
			(bytes1 :+ 4) mustEqual ImmutableBytes(Array[Byte](0, 1, 2, 3, 4))
			(3.asInstanceOf[Byte] +: bytes2) mustEqual ImmutableBytes(Array[Byte](3, 4, 5, 6, 7))
		}
	}

	"reverse" should {
		"be right" in {
			val bytes1 = ImmutableBytes(Array[Byte](0, 1, 2, 3))
			val bytes2 = bytes1.reverse

			bytes1 mustEqual ImmutableBytes(Array[Byte](0, 1, 2, 3))
			bytes2 mustEqual ImmutableBytes(Array[Byte](3, 2, 1, 0))
		}
	}
}
