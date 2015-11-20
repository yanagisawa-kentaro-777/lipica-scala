package org.lipicalabs.lipica.core.db

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.datasource.HashMapDB
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}
import org.lipicalabs.lipica.core.vm.DataWord
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class IndexedBlockStoreTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			ok
		}
	}

}
