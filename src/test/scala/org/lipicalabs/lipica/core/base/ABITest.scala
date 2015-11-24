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
			//println(funcJson1)
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
			//println(funcJson2)
			val function = CallTransaction.Function.fromJsonInterface(funcJson2)
			val ctx = CallTransaction.createCallTransaction(1, 1000000000, 1000000000, "86e0497e32a8e1d79fe38ab87dc80140df5470d9", 0, function)
			ctx.sign(ImmutableBytes(DigestUtils.digest256("974f963ee4571e86e5f9bc3b493e453db9c15e5bd19829a4ef9a790de0da0015".getBytes)))

			ctx.data.toHexString mustEqual "91888f2e"
		}
	}

}
