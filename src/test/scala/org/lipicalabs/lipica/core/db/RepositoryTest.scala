package org.lipicalabs.lipica.core.db

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.datasource.HashMapDB
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.DataWord
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class RepositoryTest extends Specification {
	sequential

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

	"test (5)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				(0 until 10).foreach {
					_ => track.increaseNonce(cow)
				}
				track.increaseNonce(horse)

				track.getNonce(cow) mustEqual BigInt(10)
				track.getNonce(horse) mustEqual BigInt(1)
				repository.getNonce(cow) mustEqual UtilConsts.Zero
				repository.getNonce(horse) mustEqual UtilConsts.Zero

				track.commit()

				track.getNonce(cow) mustEqual BigInt(10)
				track.getNonce(horse) mustEqual BigInt(1)
				repository.getNonce(cow) mustEqual BigInt(10)
				repository.getNonce(horse) mustEqual BigInt(1)
			} finally {
				repository.close()
			}
		}
	}

	"test (6)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				(0 until 10).foreach {
					_ => track.increaseNonce(cow)
				}
				track.increaseNonce(horse)

				track.getNonce(cow) mustEqual BigInt(10)
				track.getNonce(horse) mustEqual BigInt(1)
				repository.getNonce(cow) mustEqual UtilConsts.Zero
				repository.getNonce(horse) mustEqual UtilConsts.Zero

				track.rollback()

				track.getNonce(cow) mustEqual UtilConsts.Zero
				track.getNonce(horse) mustEqual UtilConsts.Zero
				repository.getNonce(cow) mustEqual UtilConsts.Zero
				repository.getNonce(horse) mustEqual UtilConsts.Zero
			} finally {
				repository.close()
			}
		}
	}

	"test (7)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				track.addBalance(cow, BigInt(10))
				track.addBalance(horse, BigInt(1))

				track.getBalance(cow).get mustEqual BigInt(10)
				track.getBalance(horse).get mustEqual BigInt(1)
				repository.getBalance(cow).isEmpty mustEqual true
				repository.getBalance(horse).isEmpty mustEqual true

				track.commit()

				track.getBalance(cow).get mustEqual BigInt(10)
				track.getBalance(horse).get mustEqual BigInt(1)
				repository.getBalance(cow).get mustEqual BigInt(10)
				repository.getBalance(horse).get mustEqual BigInt(1)
			} finally {
				repository.close()
			}
		}
	}

	"test (8)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				track.addBalance(cow, BigInt(10))
				track.addBalance(horse, BigInt(1))

				track.getBalance(cow).get mustEqual BigInt(10)
				track.getBalance(horse).get mustEqual BigInt(1)
				repository.getBalance(cow).isEmpty mustEqual true
				repository.getBalance(horse).isEmpty mustEqual true

				track.rollback()

				track.getBalance(cow).get mustEqual UtilConsts.Zero
				track.getBalance(horse).get mustEqual UtilConsts.Zero
				repository.getBalance(cow).isEmpty mustEqual true
				repository.getBalance(horse).isEmpty mustEqual true
			} finally {
				repository.close()
			}
		}
	}

	"test (9)" should {
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

				track.getStorageValue(cow, DataWord(cowKey)).get mustEqual DataWord(cowValue)
				track.getStorageValue(horse, DataWord(horseKey)).get mustEqual DataWord(horseValue)
				repository.getStorageValue(cow, DataWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, DataWord(horseKey)).isEmpty mustEqual true

				track.commit()

				track.getStorageValue(cow, DataWord(cowKey)).get mustEqual DataWord(cowValue)
				track.getStorageValue(horse, DataWord(horseKey)).get mustEqual DataWord(horseValue)
				repository.getStorageValue(cow, DataWord(cowKey)).get mustEqual DataWord(cowValue)
				repository.getStorageValue(horse, DataWord(horseKey)).get mustEqual DataWord(horseValue)
			} finally {
				repository.close()
			}
		}
	}

	"test (10)" should {
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

				track.getStorageValue(cow, DataWord(cowKey)).get mustEqual DataWord(cowValue)
				track.getStorageValue(horse, DataWord(horseKey)).get mustEqual DataWord(horseValue)
				repository.getStorageValue(cow, DataWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, DataWord(horseKey)).isEmpty mustEqual true

				track.rollback()

				track.getStorageValue(cow, DataWord(cowKey)).isEmpty mustEqual true
				track.getStorageValue(horse, DataWord(horseKey)).isEmpty mustEqual true
				repository.getStorageValue(cow, DataWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, DataWord(horseKey)).isEmpty mustEqual true
			} finally {
				repository.close()
			}
		}
	}

	"test (11)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowCode = ImmutableBytes.parseHexString("A1A2A3")
				val horseCode = ImmutableBytes.parseHexString("B1B2B3")

				track.saveCode(cow, cowCode)
				track.saveCode(horse, horseCode)

				track.getCode(cow).get mustEqual cowCode
				track.getCode(horse).get mustEqual horseCode
				repository.getCode(cow).isEmpty mustEqual true
				repository.getCode(horse).isEmpty mustEqual true

				track.commit()

				track.getCode(cow).get.isEmpty mustEqual true
				track.getCode(horse).get.isEmpty mustEqual true
				repository.getCode(cow).get mustEqual cowCode
				repository.getCode(horse).get mustEqual horseCode
			} finally {
				repository.close()
			}
		}
	}

	"test (12)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			val track = repository.startTracking
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowCode = ImmutableBytes.parseHexString("A1A2A3")
				val horseCode = ImmutableBytes.parseHexString("B1B2B3")

				track.saveCode(cow, cowCode)
				track.saveCode(horse, horseCode)

				track.getCode(cow).get mustEqual cowCode
				track.getCode(horse).get mustEqual horseCode
				repository.getCode(cow).isEmpty mustEqual true
				repository.getCode(horse).isEmpty mustEqual true

				track.rollback()

				track.getCode(cow).isEmpty mustEqual true
				track.getCode(horse).isEmpty mustEqual true
				repository.getCode(cow).isEmpty mustEqual true
				repository.getCode(horse).isEmpty mustEqual true
			} finally {
				repository.close()
			}
		}
	}

	//TODO test (13) をスキップする。

	"test (14)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val track1 = repository.startTracking
				track1.addBalance(cow, BigInt(10))
				track1.addBalance(horse, BigInt(1))

				track1.getBalance(cow).get mustEqual BigInt(10)
				track1.getBalance(horse).get mustEqual BigInt(1)

				val track2 = track1.startTracking
				track2.addBalance(cow, BigInt(1))
				track2.addBalance(horse, BigInt(10))

				track2.getBalance(cow).get mustEqual BigInt(11)
				track2.getBalance(horse).get mustEqual BigInt(11)

				track2.commit()
				track1.commit()

				repository.getBalance(cow).get mustEqual BigInt(11)
				repository.getBalance(horse).get mustEqual BigInt(11)
			} finally {
				repository.close()
			}
		}
	}

	"test (15)" should {
		"be right" in {
			val repository = new RepositoryImpl(new HashMapDB, new HashMapDB)
			try {
				val cow = ImmutableBytes.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = ImmutableBytes.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val track1 = repository.startTracking
				track1.addBalance(cow, BigInt(10))
				track1.addBalance(horse, BigInt(1))

				track1.getBalance(cow).get mustEqual BigInt(10)
				track1.getBalance(horse).get mustEqual BigInt(1)

				val track2 = track1.startTracking
				track2.addBalance(cow, BigInt(1))
				track2.addBalance(horse, BigInt(10))

				track2.getBalance(cow).get mustEqual BigInt(11)
				track2.getBalance(horse).get mustEqual BigInt(11)

				track2.rollback()
				track1.commit()

				repository.getBalance(cow).get mustEqual BigInt(10)
				repository.getBalance(horse).get mustEqual BigInt(1)
			} finally {
				repository.close()
			}
		}
	}

	//TODO (16)以下未実装。

}
