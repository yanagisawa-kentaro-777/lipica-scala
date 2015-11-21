package org.lipicalabs.lipica.core.db

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.db.datasource.HashMapDB
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.{DataWord, LogInfo}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/08
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class DatabaseImplTest extends Specification {
	sequential


	"io" should {
		"be right" in {
			val db = new DatabaseImpl(new HashMapDB)
			db.init()

			db.get(ImmutableBytes.empty).isEmpty mustEqual true
			db.sortedKeys.size mustEqual 0

			db.put(ImmutableBytes.empty, ImmutableBytes.empty)
			db.get(ImmutableBytes.empty).isEmpty mustEqual false
			db.get(ImmutableBytes.empty).get mustEqual ImmutableBytes.empty
			db.sortedKeys.size mustEqual 1

			db.delete(ImmutableBytes.empty)
			db.get(ImmutableBytes.empty).isEmpty mustEqual true
			db.sortedKeys.size mustEqual 0

			db.close()
			ok
		}
	}

}
