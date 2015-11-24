package org.lipicalabs.lipica.core.base

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class ABITest extends Specification {
	sequential

	val funcJson1: String = ("{ \n" +
		"  'constant': false, \n" +
		"  'inputs': [{'name':'to', 'type':'address'}], \n" +
		"  'name': 'delegate', \n" +
		"  'outputs': [], \n" +
		"  'type': 'function' \n" +
		"} \n").replaceAll("'", "\"")

	"test simple (1)" should {
		"be right" in {
			val function = CallTransaction.Function.fromJsonInterface(funcJson1)
			function.encode("1234567890abcdef1234567890abcdef12345678").toHexString mustEqual "5c19a95c0000000000000000000000001234567890abcdef1234567890abcdef12345678"
			function.encode("0x1234567890abcdef1234567890abcdef12345678").toHexString mustEqual "5c19a95c0000000000000000000000001234567890abcdef1234567890abcdef12345678"

			try {
				function.encode("0xa1234567890abcdef1234567890abcdef12345678")
				ko
			} catch {
				case e: Throwable => ok
			}
			try {
				function.encode("blabla")
				ko
			} catch {
				case e: Throwable => ok
			}
		}
	}

	val funcJson2: String = ("{\n" +
			" 'constant':false, \n" +
			" 'inputs':[], \n" +
			" 'name':'tst', \n" +
			" 'outputs':[], \n" +
			" 'type':'function' \n" +
			"}").replaceAll("'", "\"")

	"test simple (2)" should {
		"be right" in {
			val function = CallTransaction.Function.fromJsonInterface(funcJson2)
			val ctx = CallTransaction.createCallTransaction(1, 1000000000, 1000000000, "86e0497e32a8e1d79fe38ab87dc80140df5470d9", 0, function)
			ctx.sign(ImmutableBytes(DigestUtils.digest256("974f963ee4571e86e5f9bc3b493e453db9c15e5bd19829a4ef9a790de0da0015".getBytes)))

			ctx.data.toHexString mustEqual "91888f2e"
		}
	}

	val funcJson3: String = ("{\n" +
			" 'constant':false, \n" +
			" 'inputs':[ \n" +
			"   {'name':'i','type':'int'}, \n" +
			"   {'name':'u','type':'uint'}, \n" +
			"   {'name':'i8','type':'int8'}, \n" +
			"   {'name':'b2','type':'bytes2'}, \n" +
			"   {'name':'b32','type':'bytes32'} \n" +
			"  ], \n" +
			"  'name':'f1', \n" +
			"  'outputs':[], \n" +
			"  'type':'function' \n" +
			"}\n").replaceAll("'", "\"")

	"test simple (3)" should {
		"be right" in {
			val function = CallTransaction.Function.fromJsonInterface(funcJson3)
			function.encode(-1234, 1234, 123, "a", "the string").toHexString mustEqual "a4f72f5a" + "fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffb2e" + "00000000000000000000000000000000000000000000000000000000000004d2" + "000000000000000000000000000000000000000000000000000000000000007b61" + "000000000000000000000000000000000000000000000000000000000000007468" + "6520737472696e6700000000000000000000000000000000000000000000"
		}
	}

	private val funcJson4: String = ("{\n" +
		" 'constant':false, \n" +
		" 'inputs':[{'name':'i','type':'int[3]'}, {'name':'j','type':'int[]'}], \n" +
		" 'name':'f2', \n" +
		" 'outputs':[], \n" +
		" 'type':'function' \n" +
		"}\n").replaceAll("'", "\"")

	"test simple (4)" should {
		"be right" in {
			val function = CallTransaction.Function.fromJsonInterface(funcJson4)
			function.encode(Array[Int](1, 2, 3)).toHexString mustEqual "d383b9f6" + "0000000000000000000000000000000000000000000000000000000000000001" + "0000000000000000000000000000000000000000000000000000000000000002" + "0000000000000000000000000000000000000000000000000000000000000003"
			function.encode(Array[Int](1, 2, 3), Array[Int](4, 5)).toHexString mustEqual "d383b9f60000000000000000000000000000000000000000000000000000000000000001" + "0000000000000000000000000000000000000000000000000000000000000002" + "0000000000000000000000000000000000000000000000000000000000000003" + "0000000000000000000000000000000000000000000000000000000000000080" + "0000000000000000000000000000000000000000000000000000000000000002" + "0000000000000000000000000000000000000000000000000000000000000004" + "0000000000000000000000000000000000000000000000000000000000000005"
		}
	}

	val funcJson5: String = ("{\n" +
		"   'constant':false, \n" +
		"   'inputs':[{'name':'i','type':'int'}, \n" +
		"               {'name':'s','type':'bytes'}, \n" +
		"               {'name':'j','type':'int'}], \n" +
		"    'name':'f4', \n" + "    'outputs':[], \n" +
		"    'type':'function' \n" +
		"}\n").replaceAll("'", "\"")

	"test simple (5)" should {
		"be right" in {
			val function = CallTransaction.Function.fromJsonInterface(funcJson5)
			function.encode(111, Array[Byte](0xab.toByte, 0xcd.toByte, 0xef.toByte), 222).toHexString mustEqual "3ed2792b000000000000000000000000000000000000000000000000000000000000006f" + "0000000000000000000000000000000000000000000000000000000000000060" + "00000000000000000000000000000000000000000000000000000000000000de" + "0000000000000000000000000000000000000000000000000000000000000003" + "abcdef0000000000000000000000000000000000000000000000000000000000"
		}
	}

}
