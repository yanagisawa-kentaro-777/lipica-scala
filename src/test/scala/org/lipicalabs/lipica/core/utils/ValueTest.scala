package org.lipicalabs.lipica.core.utils

import java.nio.charset.StandardCharsets

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
			value.value.asInstanceOf[Array[Byte]] sameElements Array(0.asInstanceOf[Byte])
			(value.encodedBytes sameElements Array(0x80.asInstanceOf[Byte])) mustEqual true
			value.hash.toHexString mustEqual "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
		}
	}

	"string case (1)" should {
		"be right" in {
			val value = Value.fromObject("abc")
			value.isString mustEqual true
			value.asString mustEqual "abc"
		}
	}

	"string case (2)" should {
		"be right" in {
			val value = Value.fromObject("abc".getBytes(StandardCharsets.UTF_8))
			value.isBytes mustEqual true
			value.isString mustEqual false
			value.asString mustEqual "abc"
		}
	}


	"int cases" should {
		"be right" in {
			(0 until 10000).foreach {i => {
				val value = Value.fromObject(i)
				value.value mustEqual i
				value.isInt mustEqual true
				value.asInt mustEqual i
				value.asLong mustEqual i.toLong
				value.asBigInt.longValue mustEqual i.toLong
				val encoded = value.encodedBytes
				val rebuiltValue = Value.fromEncodedBytes(encoded)
				rebuiltValue.isInt mustEqual false
				rebuiltValue.asInt mustEqual i
				rebuiltValue.asLong mustEqual i.toLong
				rebuiltValue.asBigInt.longValue mustEqual i.toLong
			}}
			ok
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
				value.isImmutableBytes mustEqual true
				val encoded = value.encodedBytes
				val rebuiltValue = Value.fromEncodedBytes(encoded)
				rebuiltValue.asImmutableBytes mustEqual bytes
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
