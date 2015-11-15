package org.lipicalabs.lipica.core.db

import java.util.Random

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.datasource.{HashMapDB, KeyValueDataSource}
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.DataWord
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class RepositoryTest extends Specification {
	sequential

	private def randomBytes(length: Int): ImmutableBytes = {
		val random = new Random
		val result = new Array[Byte](length)
		random.nextBytes(result)
		ImmutableBytes(result)
	}

	private def randomWord: DataWord = DataWord(randomBytes(32))

	"test (1)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				repository.increaseNonce(cow)
				repository.increaseNonce(horse)

				repository.getNonce(cow) mustEqual UtilConsts.One
				repository.increaseNonce(cow)

				repository.getNonce(cow) mustEqual BigInt(2)
			} finally {
				repository.close()
			}
		}
	}

	"test (2)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				repository.addBalance(cow, BigInt(10))
				repository.addBalance(horse, UtilConsts.One)

				repository.getBalance(cow).get mustEqual BigInt(10)
				repository.getBalance(horse).get mustEqual UtilConsts.One
			} finally {
				repository.close()
			}
		}
	}

	"test (3)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowCode = ImmutableBytes.parseHexString("A1A2A3")
				val horseCode = ImmutableBytes.parseHexString("B1B2B3")

				repository.saveCode(cow, cowCode)
				repository.saveCode(horse, horseCode)

				repository.getCode(cow).get mustEqual cowCode
				repository.getCode(horse).get mustEqual horseCode
			} finally {
				repository.close()
			}
		}
	}

	"test (4)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey = ImmutableBytes.parseHexString("A1A2A3")
				val cowValue = ImmutableBytes.parseHexString("A4A5A6")

				val horseKey = ImmutableBytes.parseHexString("B1B2B3")
				val horseValue = ImmutableBytes.parseHexString("B4B5B6")

				track.addStorageRow(cow, DataWord(cowKey), DataWord(cowValue))
				track.addStorageRow(horse, DataWord(horseKey), DataWord(horseValue))
				track.commit()

				repository.getStorageValue(cow, DataWord(cowKey)).get mustEqual DataWord(cowValue)
				repository.getStorageValue(horse, DataWord(horseKey)).get mustEqual DataWord(horseValue)
			} finally {
				repository.close()
			}
		}
	}

	//TODO test (5) 以下未実装。


}
