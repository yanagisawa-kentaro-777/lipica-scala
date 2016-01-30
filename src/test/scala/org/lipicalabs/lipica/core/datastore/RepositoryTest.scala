package org.lipicalabs.lipica.core.datastore

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.datastore.datasource.InMemoryDataSource
import org.lipicalabs.lipica.core.kernel.Address
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.VMWord
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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				repository.incrementNonce(cow)
				repository.incrementNonce(horse)

				repository.getNonce(cow) mustEqual UtilConsts.One
				repository.incrementNonce(cow)

				repository.getNonce(cow) mustEqual BigInt(2)
			} finally {
				repository.close()
			}
		}
	}

	"test (2)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey = ImmutableBytes.parseHexString("A1A2A3")
				val cowValue = ImmutableBytes.parseHexString("A4A5A6")

				val horseKey = ImmutableBytes.parseHexString("B1B2B3")
				val horseValue = ImmutableBytes.parseHexString("B4B5B6")

				track.addStorageRow(cow, VMWord(cowKey), VMWord(cowValue))
				track.addStorageRow(horse, VMWord(horseKey), VMWord(horseValue))
				track.commit()

				repository.getStorageValue(cow, VMWord(cowKey)).get mustEqual VMWord(cowValue)
				repository.getStorageValue(horse, VMWord(horseKey)).get mustEqual VMWord(horseValue)
			} finally {
				repository.close()
			}
		}
	}

	"test (5)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				(0 until 10).foreach {
					_ => track.incrementNonce(cow)
				}
				track.incrementNonce(horse)

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				(0 until 10).foreach {
					_ => track.incrementNonce(cow)
				}
				track.incrementNonce(horse)

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey = ImmutableBytes.parseHexString("A1A2A3")
				val cowValue = ImmutableBytes.parseHexString("A4A5A6")

				val horseKey = ImmutableBytes.parseHexString("B1B2B3")
				val horseValue = ImmutableBytes.parseHexString("B4B5B6")

				track.addStorageRow(cow, VMWord(cowKey), VMWord(cowValue))
				track.addStorageRow(horse, VMWord(horseKey), VMWord(horseValue))

				track.getStorageValue(cow, VMWord(cowKey)).get mustEqual VMWord(cowValue)
				track.getStorageValue(horse, VMWord(horseKey)).get mustEqual VMWord(horseValue)
				repository.getStorageValue(cow, VMWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, VMWord(horseKey)).isEmpty mustEqual true

				track.commit()

				track.getStorageValue(cow, VMWord(cowKey)).get mustEqual VMWord(cowValue)
				track.getStorageValue(horse, VMWord(horseKey)).get mustEqual VMWord(horseValue)
				repository.getStorageValue(cow, VMWord(cowKey)).get mustEqual VMWord(cowValue)
				repository.getStorageValue(horse, VMWord(horseKey)).get mustEqual VMWord(horseValue)
			} finally {
				repository.close()
			}
		}
	}

	"test (10)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey = ImmutableBytes.parseHexString("A1A2A3")
				val cowValue = ImmutableBytes.parseHexString("A4A5A6")

				val horseKey = ImmutableBytes.parseHexString("B1B2B3")
				val horseValue = ImmutableBytes.parseHexString("B4B5B6")

				track.addStorageRow(cow, VMWord(cowKey), VMWord(cowValue))
				track.addStorageRow(horse, VMWord(horseKey), VMWord(horseValue))

				track.getStorageValue(cow, VMWord(cowKey)).get mustEqual VMWord(cowValue)
				track.getStorageValue(horse, VMWord(horseKey)).get mustEqual VMWord(horseValue)
				repository.getStorageValue(cow, VMWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, VMWord(horseKey)).isEmpty mustEqual true

				track.rollback()

				track.getStorageValue(cow, VMWord(cowKey)).isEmpty mustEqual true
				track.getStorageValue(horse, VMWord(horseKey)).isEmpty mustEqual true
				repository.getStorageValue(cow, VMWord(cowKey)).isEmpty mustEqual true
				repository.getStorageValue(horse, VMWord(horseKey)).isEmpty mustEqual true
			} finally {
				repository.close()
			}
		}
	}

	"test (11)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowCode = ImmutableBytes.parseHexString("A1A2A3")
				val horseCode = ImmutableBytes.parseHexString("B1B2B3")

				track.saveCode(cow, cowCode)
				track.saveCode(horse, horseCode)

				track.getCode(cow).get mustEqual cowCode
				track.getCode(horse).get mustEqual horseCode
				repository.getCode(cow).isEmpty mustEqual true
				repository.getCode(horse).isEmpty mustEqual true

				track.commit()

				track.getCode(cow).get mustEqual cowCode
				track.getCode(horse).get mustEqual horseCode
				repository.getCode(cow).get mustEqual cowCode
				repository.getCode(horse).get mustEqual horseCode
			} finally {
				repository.close()
			}
		}
	}

	"test (12)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			val track = repository.startTracking
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

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

	"test (16)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey1 = ImmutableBytes("key-c-1".getBytes)
				val cowValue1 = ImmutableBytes("val-c-1".getBytes)

				val horseKey1 = ImmutableBytes("key-h-1".getBytes)
				val horseValue1 = ImmutableBytes("val-h-1".getBytes)

				val cowKey2 = ImmutableBytes("key-c-2".getBytes)
				val cowValue2 = ImmutableBytes("val-c-2".getBytes)

				val horseKey2 = ImmutableBytes("key-h-2".getBytes)
				val horseValue2 = ImmutableBytes("val-h-2".getBytes)

				val track1 = repository.startTracking
				track1.addStorageRow(cow, VMWord(cowKey1), VMWord(cowValue1))
				track1.addStorageRow(horse, VMWord(horseKey1), VMWord(horseValue1))

				val track2 = track1.startTracking
				track2.addStorageRow(cow, VMWord(cowKey2), VMWord(cowValue2))
				track2.addStorageRow(horse, VMWord(horseKey2), VMWord(horseValue2))

				track2.getStorageValue(cow, VMWord(cowKey1)).get mustEqual VMWord(cowValue1)
				track2.getStorageValue(horse, VMWord(horseKey1)).get mustEqual VMWord(horseValue1)
				track2.getStorageValue(cow, VMWord(cowKey2)).get mustEqual VMWord(cowValue2)
				track2.getStorageValue(horse, VMWord(horseKey2)).get mustEqual VMWord(horseValue2)

				track2.commit()

				track1.getStorageValue(cow, VMWord(cowKey1)).get mustEqual VMWord(cowValue1)
				track1.getStorageValue(horse, VMWord(horseKey1)).get mustEqual VMWord(horseValue1)
				track1.getStorageValue(cow, VMWord(cowKey2)).get mustEqual VMWord(cowValue2)
				track1.getStorageValue(horse, VMWord(horseKey2)).get mustEqual VMWord(horseValue2)

				track1.commit()

				repository.getStorageValue(cow, VMWord(cowKey1)).get mustEqual VMWord(cowValue1)
				repository.getStorageValue(horse, VMWord(horseKey1)).get mustEqual VMWord(horseValue1)
				repository.getStorageValue(cow, VMWord(cowKey2)).get mustEqual VMWord(cowValue2)
				repository.getStorageValue(horse, VMWord(horseKey2)).get mustEqual VMWord(horseValue2)

			} finally {
				repository.close()
			}
		}
	}

	//TODO (16-2)以下スキップ。

	"test (17)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey1 = ImmutableBytes("key-c-1".getBytes)
				val cowValue1 = ImmutableBytes("val-c-1".getBytes)

				val track1 = repository.startTracking

				val track2 = track1.startTracking
				track2.addStorageRow(cow, VMWord(cowKey1), VMWord(cowValue1))
				track2.getStorageValue(cow, VMWord(cowKey1)).get mustEqual VMWord(cowValue1)

				track2.rollback()
				track1.commit()

				repository.getStorageValue(cow, VMWord(cowKey1)).isEmpty mustEqual true
			} finally {
				repository.close()
			}
		}
	}

	"test (18)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")
				val pig = Address.parseHexString("F0B8C9D84DD2B877E0B952130B73E218106FEC04")
				val precompiled = Address.parseHexString("0000000000000000000000000000000000000002")

				val track = repository.startTracking

				val cowCode = ImmutableBytes.parseHexString("A1A2A3")
				val horseCode = ImmutableBytes.parseHexString("B1B2B3")

				repository.saveCode(cow, cowCode)
				repository.saveCode(horse, horseCode)

				repository.delete(horse)

				track.existsAccount(cow) mustEqual true
				track.existsAccount(horse) mustEqual false
				track.existsAccount(pig) mustEqual false
				track.existsAccount(precompiled) mustEqual false
			} finally {
				repository.close()
			}
		}
	}

	"test (19)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey1 = VMWord(ImmutableBytes("ck1".getBytes))
				val cowVal1 = VMWord(ImmutableBytes("cv1".getBytes))
				val cowVal0 = VMWord(ImmutableBytes("cv0".getBytes))

				val horseKey1 = VMWord(ImmutableBytes("hk1".getBytes))
				val horseVal1 = VMWord(ImmutableBytes("hv1".getBytes))
				val horseVal0 = VMWord(ImmutableBytes("hv0".getBytes))

				val track = repository.startTracking
				track.addStorageRow(cow, cowKey1, cowVal0)
				track.addStorageRow(horse, horseKey1, horseVal0)
				track.commit()

				val track2 = repository.startTracking
				track2.addStorageRow(horse, horseKey1, horseVal0)

				val track3 = track2.startTracking

				val cowDetails = track3.getContractDetails(cow).get
				cowDetails.put(cowKey1, cowVal1)

				val horseDetails = track3.getContractDetails(horse).get
				horseDetails.put(horseKey1, horseVal1)

				track3.commit()
				track2.rollback()

				val cowDetailsOrigin = repository.getContractDetails(cow).get
				val cowValOrigin = cowDetailsOrigin.get(cowKey1).get

				val horseDetailsOrigin = repository.getContractDetails(horse).get
				val horseValOrgin = horseDetailsOrigin.get(horseKey1).get

				cowValOrigin mustEqual cowVal0
				horseValOrgin mustEqual horseVal0
			} finally {
				repository.close()
			}
		}
	}

	"test (20)" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val root = repository.rootHash

				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")
				val horse = Address.parseHexString("13978AEE95F38490E9769C39B2773ED763D9CD5F")

				val cowKey1 = VMWord(ImmutableBytes("ck1".getBytes))
				val cowKey2 = VMWord(ImmutableBytes("ck2".getBytes))
				val cowVal1 = VMWord(ImmutableBytes("cv1".getBytes))
				val cowVal0 = VMWord(ImmutableBytes("cv0".getBytes))

				val horseKey1 = VMWord(ImmutableBytes("hk1".getBytes))
				val horseKey2 = VMWord(ImmutableBytes("hk2".getBytes))
				val horseVal1 = VMWord(ImmutableBytes("hv1".getBytes))
				val horseVal0 = VMWord(ImmutableBytes("hv0".getBytes))



				var track2 = repository.startTracking
				track2.addStorageRow(cow, cowKey1, cowVal1)
				track2.addStorageRow(horse, horseKey1, horseVal1)
				track2.commit()

				val root2 = repository.rootHash

				track2 = repository.startTracking
				track2.addStorageRow(cow, cowKey2, cowVal0)
				track2.addStorageRow(horse, horseKey2, horseVal0)
				track2.commit()

				val root3 = repository.rootHash

				var snapshot = repository.createSnapshotTo(root)
				var cowDetails = snapshot.getContractDetails(cow).get
				var horseDetails = snapshot.getContractDetails(horse).get
				cowDetails.get(cowKey1).isEmpty mustEqual true
				cowDetails.get(cowKey2).isEmpty mustEqual true
				horseDetails.get(horseKey1).isEmpty mustEqual true
				horseDetails.get(horseKey2).isEmpty mustEqual true

				snapshot = repository.createSnapshotTo(root2)
				cowDetails = snapshot.getContractDetails(cow).get
				horseDetails = snapshot.getContractDetails(horse).get

				cowDetails.get(cowKey1).get mustEqual cowVal1
				cowDetails.get(cowKey2).isEmpty mustEqual true
				horseDetails.get(horseKey1).get mustEqual horseVal1
				horseDetails.get(horseKey2).isEmpty mustEqual true

				snapshot = repository.createSnapshotTo(root3)
				cowDetails = snapshot.getContractDetails(cow).get
				horseDetails = snapshot.getContractDetails(horse).get

				cowDetails.get(cowKey1).get mustEqual cowVal1
				cowDetails.get(cowKey2).get mustEqual cowVal0
				horseDetails.get(horseKey1).get mustEqual horseVal1
				horseDetails.get(horseKey2).get mustEqual horseVal0
			} finally {
				repository.close()
			}
		}
	}

	"manage accounts by track" should {
		"be right" in {
			val repository = new RepositoryImpl(new InMemoryDataSource, new InMemoryDataSource, new InMemoryDataSourceFactory)
			try {
				val track = repository.startTracking
				val cow = Address.parseHexString("CD2A3D9F938E13CD947EC05ABC7FE734DF8DD826")

				track.existsAccount(cow) mustEqual false
				track.createAccount(cow)
				track.existsAccount(cow) mustEqual true
				repository.existsAccount(cow) mustEqual false
				track.commit()
				track.existsAccount(cow) mustEqual true
				repository.existsAccount(cow) mustEqual true

				track.delete(cow)

				track.existsAccount(cow) mustEqual false
				repository.existsAccount(cow) mustEqual true

				track.commit()
				repository.existsAccount(cow) mustEqual false
			} finally {
				repository.close()
			}
		}
	}
}
