package org.lipicalabs.lipica.utils

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class VersionTest extends Specification {
sequential

	"attributes (1)" should {
		"be right" in {
			val v1 = Version.parse("1.2.3-SNAPSHOT").right.get
			v1.toCanonicalString mustEqual "1.2.3-SNAPSHOT"

			val v2 = Version(1, 2, 3, Option("SNAPSHOT"), None)
			v1 mustEqual v2

			v1.compareTo(v2) mustEqual 0

			val v3 = Version(1, 2, 4, Option("SNAPSHOT"), Option("20160101"))

			v2.compareTo(v3) mustEqual -1
			v3.toString mustEqual "1.2.4-SNAPSHOT BUILD:20160101"
		}
	}


}
