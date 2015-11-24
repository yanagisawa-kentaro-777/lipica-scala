package org.lipicalabs.lipica.core.base

import org.junit.runner.RunWith
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
		"  'inputs': [{'name':'to', 'paramType':'address'}], \n" +
		"  'name': 'delegate', \n" +
		"  'outputs': [], \n" +
		"  'functionType': 'function' \n" +
		"} \n").replaceAll("'", "\"")

	"test simple (1)" should {
		"be right" in {
			println(funcJson1)
			val function = CallTransaction.Function.fromJsonInterface(funcJson1)
			function.encode("1234567890abcdef1234567890abcdef12345678").toHexString mustEqual "5c19a95c0000000000000000000000001234567890abcdef1234567890abcdef12345678"
		}
	}


}
