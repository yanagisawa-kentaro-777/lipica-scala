package org.lipicalabs.lipica.core.utils

import org.apache.commons.codec.binary.Hex
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
			value.decode mustEqual 0
			(value.encode sameElements Array(0x80.asInstanceOf[Byte])) mustEqual true
			Hex.encodeHexString(value.hash) mustEqual "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
		}
	}

	"instance creation (2)" should {
		"be right" in {
			val value = Value.fromEncodedBytes(Array(0x80.asInstanceOf[Byte]))
			value.value.asInstanceOf[Array[Byte]] sameElements Array(0.asInstanceOf[Byte])
			value.decode.asInstanceOf[Array[Byte]] sameElements Array(0.asInstanceOf[Byte])
			(value.encode sameElements Array(0x80.asInstanceOf[Byte])) mustEqual true
			Hex.encodeHexString(value.hash) mustEqual "56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"
		}
	}

}
