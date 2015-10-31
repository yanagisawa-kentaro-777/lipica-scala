package org.lipicalabs.lipica.core.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ImmutableBytesTest extends Specification {
sequential

	private val T = 16.toByte

	"elements (1)" should {
		"be right" in {
			val test1 = ImmutableBytes(Array[Byte](1, 2, 3, 4, 5))
			test1.length mustEqual 5
			test1.size mustEqual 5
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
		}
	}

	"elements (2)" should {
		"be right" in {
			val test = ImmutableBytes(null.asInstanceOf[Array[Byte]])
			test.length mustEqual 0
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
}
