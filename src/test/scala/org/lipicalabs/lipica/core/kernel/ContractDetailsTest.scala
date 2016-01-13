package org.lipicalabs.lipica.core.kernel

import java.util.Random

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.db.HashMapDBFactory
import org.lipicalabs.lipica.core.db.datasource.{HashMapDB, KeyValueDataSource}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
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
class ContractDetailsTest extends Specification {
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
			val code = ImmutableBytes.parseHexString("60016002")

			val key_1 = ImmutableBytes.parseHexString("111111")
			val val_1 = ImmutableBytes.parseHexString("aaaaaa")

			val key_2 = ImmutableBytes.parseHexString("222222")
			val val_2 = ImmutableBytes.parseHexString("bbbbbb")

			val contractDetails = new ContractDetailsImpl(new HashMapDBFactory)
			contractDetails.code = code
			contractDetails.put(DataWord(key_1), DataWord(val_1))
			contractDetails.put(DataWord(key_2), DataWord(val_2))

			val encoded = contractDetails.encode
			val decodedDetails = ContractDetailsImpl.decode(encoded, new HashMapDBFactory)

			code.toHexString mustEqual decodedDetails.code.toHexString
			val_1.toHexString mustEqual decodedDetails.get(DataWord(key_1)).get.getDataWithoutLeadingZeros.toHexString
			val_2.toHexString mustEqual decodedDetails.get(DataWord(key_2)).get.getDataWithoutLeadingZeros.toHexString
		}
	}

	"test (2)" should {
		"be right" in {
			val code = ImmutableBytes.parseHexString("7c0100000000000000000000000000000000000000000000000000000000600035046333d546748114610065578063430fe5f01461007c5780634d432c1d1461008d578063501385b2146100b857806357eb3b30146100e9578063dbc7df61146100fb57005b6100766004356024356044356102f0565b60006000f35b61008760043561039e565b60006000f35b610098600435610178565b8073ffffffffffffffffffffffffffffffffffffffff1660005260206000f35b6100c96004356024356044356101a0565b8073ffffffffffffffffffffffffffffffffffffffff1660005260206000f35b6100f1610171565b8060005260206000f35b610106600435610133565b8360005282602052816040528073ffffffffffffffffffffffffffffffffffffffff1660605260806000f35b5b60006020819052908152604090208054600182015460028301546003909301549192909173ffffffffffffffffffffffffffffffffffffffff1684565b5b60015481565b5b60026020526000908152604090205473ffffffffffffffffffffffffffffffffffffffff1681565b73ffffffffffffffffffffffffffffffffffffffff831660009081526020819052604081206002015481908302341080156101fe575073ffffffffffffffffffffffffffffffffffffffff8516600090815260208190526040812054145b8015610232575073ffffffffffffffffffffffffffffffffffffffff85166000908152602081905260409020600101548390105b61023b57610243565b3391506102e8565b6101966103ca60003973ffffffffffffffffffffffffffffffffffffffff3381166101965285166101b68190526000908152602081905260408120600201546101d6526101f68490526102169080f073ffffffffffffffffffffffffffffffffffffffff8616600090815260208190526040902060030180547fffffffffffffffffffffffff0000000000000000000000000000000000000000168217905591508190505b509392505050565b73ffffffffffffffffffffffffffffffffffffffff33166000908152602081905260408120548190821461032357610364565b60018054808201909155600090815260026020526040902080547fffffffffffffffffffffffff000000000000000000000000000000000000000016331790555b50503373ffffffffffffffffffffffffffffffffffffffff1660009081526020819052604090209081556001810192909255600290910155565b3373ffffffffffffffffffffffffffffffffffffffff166000908152602081905260409020600201555600608061019660043960048051602451604451606451600080547fffffffffffffffffffffffff0000000000000000000000000000000000000000908116909517815560018054909516909317909355600355915561013390819061006390396000f3007c0100000000000000000000000000000000000000000000000000000000600035046347810fe381146100445780637e4a1aa81461005557806383d2421b1461006957005b61004f6004356100ab565b60006000f35b6100636004356024356100fc565b60006000f35b61007460043561007a565b60006000f35b6001543373ffffffffffffffffffffffffffffffffffffffff9081169116146100a2576100a8565b60078190555b50565b73ffffffffffffffffffffffffffffffffffffffff8116600090815260026020526040902080547fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff0016600117905550565b6001543373ffffffffffffffffffffffffffffffffffffffff9081169116146101245761012f565b600582905560068190555b505056")
			val address = randomBytes(32)

			val key_0 = ImmutableBytes.parseHexString("39a2338cbc13ff8523a9b1c9bc421b7518d63b70aa690ad37cb50908746c9a55")
			val val_0 = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000064")

			val key_1 = ImmutableBytes.parseHexString("39a2338cbc13ff8523a9b1c9bc421b7518d63b70aa690ad37cb50908746c9a56")
			val val_1 = ImmutableBytes.parseHexString("000000000000000000000000000000000000000000000000000000000000000c")

			val key_2 = ImmutableBytes.parseHexString("4effac3ed62305246f40d058e1a9a8925a448d1967513482947d1d3f6104316f")
			val val_2 = ImmutableBytes.parseHexString("7a65703300000000000000000000000000000000000000000000000000000000")

			val key_3 = ImmutableBytes.parseHexString("4effac3ed62305246f40d058e1a9a8925a448d1967513482947d1d3f61043171")
			val val_3 = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000014")

			val key_4 = ImmutableBytes.parseHexString("39a2338cbc13ff8523a9b1c9bc421b7518d63b70aa690ad37cb50908746c9a54")
			val val_4 = ImmutableBytes.parseHexString("7a65703200000000000000000000000000000000000000000000000000000000")

			val key_5 = ImmutableBytes.parseHexString("4effac3ed62305246f40d058e1a9a8925a448d1967513482947d1d3f61043170")
			val val_5 = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000078")

			val key_6 = ImmutableBytes.parseHexString("e90b7bceb6e7df5418fb78d8ee546e97c83a08bbccc01a0644d599ccd2a7c2e0")
			val val_6 = ImmutableBytes.parseHexString("00000000000000000000000010b426278fbec874791c4e3f9f48a59a44686efe")

			val key_7 = ImmutableBytes.parseHexString("0df3cc3597c5ede0b1448e94daf1f1445aa541c6c03f602a426f04ae47508bb8")
			val val_7 = ImmutableBytes.parseHexString("7a65703100000000000000000000000000000000000000000000000000000000")

			val key_8 = ImmutableBytes.parseHexString("0df3cc3597c5ede0b1448e94daf1f1445aa541c6c03f602a426f04ae47508bb9")
			val val_8 = ImmutableBytes.parseHexString("00000000000000000000000000000000000000000000000000000000000000c8")

			val key_9 = ImmutableBytes.parseHexString("0df3cc3597c5ede0b1448e94daf1f1445aa541c6c03f602a426f04ae47508bba")
			val val_9 = ImmutableBytes.parseHexString("000000000000000000000000000000000000000000000000000000000000000a")

			val key_10 = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000001")
			val val_10 = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000003")

			val key_11 = ImmutableBytes.parseHexString("0df3cc3597c5ede0b1448e94daf1f1445aa541c6c03f602a426f04ae47508bbb")
			val val_11 = ImmutableBytes.parseHexString("0000000000000000000000007cd917d6194bcfc3670d8a1613e5b0c790036a35")

			val key_12 = ImmutableBytes.parseHexString("679795a0195a1b76cdebb7c51d74e058aee92919b8c3389af86ef24535e8a28c")
			val val_12 = ImmutableBytes.parseHexString("000000000000000000000000b0b0a72fcfe293a85bef5915e1a7acb37bf0c685")

			val key_13 = ImmutableBytes.parseHexString("ac33ff75c19e70fe83507db0d683fd3465c996598dc972688b7ace676c89077b")
			val val_13 = ImmutableBytes.parseHexString("0000000000000000000000000c6686f3d6ee27e285f2de7b68e8db25cf1b1063")

			val contractDetails = new ContractDetailsImpl(new HashMapDBFactory)
			contractDetails.code = code
			contractDetails.address = address
			contractDetails.put(key_0, val_0)
			contractDetails.put(key_1, val_1)
			contractDetails.put(key_2, val_2)
			contractDetails.put(key_3, val_3)
			contractDetails.put(key_4, val_4)
			contractDetails.put(key_5, val_5)
			contractDetails.put(key_6, val_6)
			contractDetails.put(key_7, val_7)
			contractDetails.put(key_8, val_8)
			contractDetails.put(key_9, val_9)
			contractDetails.put(key_10, val_10)
			contractDetails.put(key_11, val_11)
			contractDetails.put(key_12, val_12)
			contractDetails.put(key_13, val_13)

			val encodedBytes = contractDetails.encode
			val decodedDetails = ContractDetailsImpl.decode(encodedBytes, new HashMapDBFactory)

			code.toHexString mustEqual decodedDetails.code.toHexString
			address.toHexString mustEqual decodedDetails.address.toHexString
			val_0.toHexString mustEqual decodedDetails.get(DataWord(key_0)).get.data.toHexString
			val_1.toHexString mustEqual decodedDetails.get(DataWord(key_1)).get.data.toHexString
			val_2.toHexString mustEqual decodedDetails.get(DataWord(key_2)).get.data.toHexString
			val_3.toHexString mustEqual decodedDetails.get(DataWord(key_3)).get.data.toHexString
			val_4.toHexString mustEqual decodedDetails.get(DataWord(key_4)).get.data.toHexString
			val_5.toHexString mustEqual decodedDetails.get(DataWord(key_5)).get.data.toHexString
			val_6.toHexString mustEqual decodedDetails.get(DataWord(key_6)).get.data.toHexString
			val_7.toHexString mustEqual decodedDetails.get(DataWord(key_7)).get.data.toHexString
			val_8.toHexString mustEqual decodedDetails.get(DataWord(key_8)).get.data.toHexString
			val_9.toHexString mustEqual decodedDetails.get(DataWord(key_9)).get.data.toHexString
			val_10.toHexString mustEqual decodedDetails.get(DataWord(key_10)).get.data.toHexString
			val_11.toHexString mustEqual decodedDetails.get(DataWord(key_11)).get.data.toHexString
			val_12.toHexString mustEqual decodedDetails.get(DataWord(key_12)).get.data.toHexString
			val_13.toHexString mustEqual decodedDetails.get(DataWord(key_13)).get.data.toHexString
		}
	}

	"test external storage serialization" should {
		"be right" in {
			val address = randomBytes(20)
			val code = randomBytes(512)

			val elements = new mutable.HashMap[DataWord, DataWord]
			val externalStorage = new HashMapDB

			val original = new ContractDetailsImpl(new HashMapDBFactory)
			original.externalStorageDataSource = externalStorage
			original.address = address
			original.code = code

			(0 until SystemProperties.CONFIG.detailsInMemoryStorageLimit + 10).foreach {
				_ => {
					val key = randomWord
					val value = randomWord
					elements.put(key, value)
					original.put(key, value)
				}
			}
			original.syncStorage()

			val encodedBytes = original.encode
			val decodedDetails = new ContractDetailsImpl(new HashMapDBFactory)
			decodedDetails.externalStorageDataSource = externalStorage
			decodedDetails.decode(encodedBytes)

			code.toHexString mustEqual decodedDetails.code.toHexString
			address.toHexString mustEqual decodedDetails.address.toHexString

			val storageContent = decodedDetails.storageContent
			storageContent.size mustEqual elements.size

			for (entry <- storageContent) {
				val (key, value) = entry
				value mustEqual elements.get(key).get
			}
			ok
		}
	}

	"test external storage transition" should {
		"be right" in {
			val address = randomBytes(20)
			val code = randomBytes(512)

			val elements = new mutable.HashMap[DataWord, DataWord]
			val externalStorage = new HashMapDB

			val original = new ContractDetailsImpl(new HashMapDBFactory)
			original.externalStorageDataSource = externalStorage
			original.address = address
			original.code = code

			(0 until SystemProperties.CONFIG.detailsInMemoryStorageLimit - 1).foreach {
				_ => {
					val key = randomWord
					val value = randomWord
					elements.put(key, value)
					original.put(key, value)
				}
			}
			original.syncStorage()
			//まだ外部ストレージには書かれない。
			externalStorage.getAddedItems mustEqual 0

			//エンコードおよびデコードを通す。
			val encodedBytes = original.encode
			val decodedDetails = deserialize(encodedBytes, externalStorage)

			//メモリ上の限界を超えて追加する。
			(0 until 10).foreach {
				_ => {
					val key = randomWord
					val value = randomWord
					elements.put(key, value)
					decodedDetails.put(key, value)
				}
			}
			decodedDetails.syncStorage()
			//今度は外部ストレージに書かれている。
			(0 < externalStorage.getAddedItems) mustEqual true

			//再度エンコードおよびデコードを通す。
			val decodedDetails2 = deserialize(decodedDetails.encode, externalStorage)
			val storageContent = decodedDetails2.storageContent
			storageContent.size mustEqual elements.size
			for (entry <- storageContent) {
				val (key, value) = entry
				value mustEqual elements.get(key).get
			}
			ok
		}
	}

	private def deserialize(encodedBytes: ImmutableBytes, externalStorage: KeyValueDataSource): ContractDetails = {
		val result = new ContractDetailsImpl(new HashMapDBFactory)
		result.externalStorageDataSource = externalStorage
		result.decode(encodedBytes)
		result
	}
}
