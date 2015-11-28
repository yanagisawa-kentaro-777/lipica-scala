package org.lipicalabs.lipica.core.base


import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.db.{BlockInfo, IndexedBlockStore, RepositoryImpl}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class AccountTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val account = new Account
			account.init()
			account.address.length mustEqual 20
		}

	}

}
