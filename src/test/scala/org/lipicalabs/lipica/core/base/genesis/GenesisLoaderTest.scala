package org.lipicalabs.lipica.core.base.genesis

import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class GenesisLoaderTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			GenesisLoader.loadGenesisBlock
			ok
		}
	}

}
