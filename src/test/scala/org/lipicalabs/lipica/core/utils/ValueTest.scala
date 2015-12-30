package org.lipicalabs.lipica.core.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class ValueTest extends Specification {
	sequential

	"instance creation (1)" should {
		"be right" in {
			val value = Value.fromObject(0)
			value.value mustEqual 0
			(value.encodedBytes sameElements Array(0x80.asInstanceOf[Byte])) mustEqual true
			value.hash.toHexString mustEqual "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
			value.toString.nonEmpty mustEqual true
		}
	}

	"instance creation (2)" should {
		"be right" in {
			val value = Value.fromEncodedBytes(ImmutableBytes.fromOneByte(0x80.asInstanceOf[Byte]))
			value.value.asInstanceOf[ImmutableBytes].toByteArray sameElements Array(0.asInstanceOf[Byte])
			(value.encodedBytes sameElements Array(0x80.asInstanceOf[Byte])) mustEqual true
			value.hash.toHexString mustEqual "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
		}
	}

	"seq cases" should {
		"be right" in {
			(0 until 100).foreach {i => {
				val seq = 0 until i
				val value = Value.fromObject(seq)
				value.value.asInstanceOf[Seq[Int]] mustEqual seq
				value.isSeq mustEqual true
				val encoded = value.encodedBytes
				val rebuiltValue = Value.fromEncodedBytes(encoded)
				rebuiltValue.isSeq mustEqual true
				rebuiltValue.asSeq.size mustEqual seq.size
			}}
			ok
		}
	}

	"byte array cases" should {
		"be right" in {
			(0 until 10000).foreach {i => {
				val bytes = generateByteArray(i)
				val value = Value.fromObject(bytes)
				value.value mustEqual bytes
				value.isBytes mustEqual true
				val encoded = value.encodedBytes
				val rebuiltValue = Value.fromEncodedBytes(encoded)
				if (i != 1) {
					rebuiltValue.asBytes mustEqual bytes
				}
			}}
			ok
		}
	}

	private def generateByteArray(length: Int): ImmutableBytes = {
		val result = new Array[Byte](length)
		result.indices.foreach {i => {
			result(i) = (i % 256).toByte
		}}
		ImmutableBytes(result)
	}

}
