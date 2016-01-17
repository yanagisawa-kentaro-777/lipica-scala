package org.lipicalabs.lipica.core.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class BigIntBytesTest extends Specification {
sequential

	"increments (1)" should {
		"be right" in {
			var value = BigIntBytes(UtilConsts.Zero)
			(0 until 10).foreach {
				_ => value = value.increment
			}
			value.positiveBigInt.longValue() mustEqual 10
		}
	}


}
