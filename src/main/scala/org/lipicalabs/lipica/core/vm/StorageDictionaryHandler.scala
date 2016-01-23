package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.kernel.ContractDetails
import org.lipicalabs.lipica.core.utils.{ErrorLogger, ImmutableBytes}
import org.slf4j.{LoggerFactory, Logger}

import scala.collection.mutable

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class StorageDictionaryHandler(ownerAddress: VMWord) {

	import StorageDictionaryHandler._

	private val contractAddress = ownerAddress.getDataWithoutLeadingZeros

	private val ConstHashes = {
		var storageIndex = VMWord.Zero
		(0 until 5000).map(i => {
			val digest = storageIndex.data.digest256.bytes
			val entry = new Entry(VMWord(digest), storageIndex.data)
			storageIndex = storageIndex + VMWord.One
			getMapKey(digest) -> entry
		}).toMap
	}
	private val hashes = new mutable.HashMap[ImmutableBytes, Entry]
	private val storeKeys = new mutable.HashMap[ImmutableBytes, VMWord]

	def vmSha3Notify(in: ImmutableBytes, out: VMWord) {
		try {
			hashes.put(getMapKey(out.data), new Entry(out, in))
		} catch {
			case e: Throwable =>
				//例外は伝播させない。
				ErrorLogger.logger.warn("<StorageDictHandler> Unexpected exception: ", e)
				logger.warn("<StorageDictHandler> Unexpected exception: ", e)
		}
	}

	def vmSStoreNotify(key: VMWord, value: VMWord) {
		try {
			storeKeys.put(key.data, value)
		} catch {
			case e: Throwable =>
				logger.error("Unexpected exception: ", e)
		}
	}

	private def findHash(key: ImmutableBytes): StorageDictionaryHandler.Entry = {
		val mapKey = getMapKey(key)
		hashes.getOrElse(mapKey, ConstHashes.get(mapKey).orNull)
	}

	/*
	def getKeyOriginSerpent(key: ImmutableBytes): Array[StorageDictionary.PathElement] = {
		val entry: StorageDictionaryHandler.Entry = findHash(key)
		if (entry != null) {
			if (entry.input.length > 32 && entry.input.length % 32 == 0 && Arrays.equals(key, entry.hashValue.getData)) {
				val pathLength: Int = entry.input.length / 32
				val ret: Array[StorageDictionary.PathElement] = new Array[StorageDictionary.PathElement](pathLength)
				{
					var i: Int = 0
					while (i < ret.length) {
						{
							ret(i) = guessPathElement(Arrays.copyOfRange(entry.input, i * 32, (i + 1) * 32))(0)
							ret(i).`type` = StorageDictionary.Type.MapKey
						}
						({
							i += 1; i - 1
						})
					}
				}
				return ret
			}
			else {
			}
		}
		val storageIndex: Array[StorageDictionary.PathElement] = guessPathElement(key)
		storageIndex(0).`type` = StorageDictionary.Type.StorageIndex
		return storageIndex
	}

	def getKeyOriginSolidity(key: ImmutableBytes): Array[StorageDictionary.PathElement] = {
		val entry: StorageDictionaryHandler.Entry = findHash(key)
		if (entry == null) {
			val storageIndex: Array[StorageDictionary.PathElement] = guessPathElement(key)
			storageIndex(0).`type` = StorageDictionary.Type.StorageIndex
			return storageIndex
		}
		else {
			val subKey: ImmutableBytes = Arrays.copyOfRange(entry.input, 0, entry.input.length - 32)
			val offset: Long = new BigInteger(key).subtract(new BigInteger(entry.hashValue.clone.getData)).longValue
			return Utils.mergeArrays(getKeyOriginSolidity(Arrays.copyOfRange(entry.input, entry.input.length - 32, entry.input.length)), guessPathElement(subKey), Array[StorageDictionary.PathElement](new StorageDictionary.PathElement(if (subKey.length == 0) StorageDictionary.Type.ArrayIndex else StorageDictionary.Type.Offset, offset.toInt)))
		}
	}

	def guessPathElement(bytes: ImmutableBytes): Array[StorageDictionary.PathElement] = {
		if (bytes.length == 0) return new Array[StorageDictionary.PathElement](0)
		val value: AnyRef = guessValue(bytes)
		var el: StorageDictionary.PathElement = null
		if (value.isInstanceOf[String]) {
			el = new StorageDictionary.PathElement(value.asInstanceOf[String])
		}
		else if (value.isInstanceOf[BigInteger]) {
			val bi: BigInteger = value.asInstanceOf[BigInteger]
			if (bi.bitLength < 32) el = new StorageDictionary.PathElement(StorageDictionary.Type.MapKey, bi.intValue)
			else el = new StorageDictionary.PathElement("0x" + bi.toString(16))
		}
		return Array[StorageDictionary.PathElement](el)
	}
	*/

	def guessValue(bytes: ImmutableBytes): AnyRef = {
		import scala.util.control.Breaks
		val b = new Breaks

		var startZeroCnt = 0
		var startNonZeroCnt = 0
		var asciiOnly = true

		b.breakable {
			(0 until bytes.length).foreach { i => {
				if (bytes(i) != 0) {
					if ((0 < startNonZeroCnt) || (i == 0)) {
						//連続の非ゼロ。
						startNonZeroCnt += 1
					} else {
						b.break()
					}
				} else {
					//連続のゼロ。
					if ((0 < startZeroCnt) || (i == 0)) {
						startZeroCnt += 1
					} else {
						b.break()
					}
				}
				asciiOnly &= (0x1F < bytes(i)) && (bytes(i) <= 0x7E)
			}}
		}

		var endZeroCnt = 0
		var endNonZeroCnt = 0
		b.breakable {
			(0 until bytes.length).foreach { i => {
				if (bytes(bytes.length - i - 1) != 0) {
					if ((0 < endNonZeroCnt) || (i == 0)) {
						//連続の非ゼロ。
						endNonZeroCnt += 1
					} else {
						b.break()
					}
				} else {
					if ((0 < endZeroCnt) || (i == 0)) {
						endZeroCnt += 1
					} else {
						b.break()
					}
				}
			}}
		}

		if (16 < startZeroCnt) {
			bytes.toSignedBigInt
		} else if (asciiOnly) {
			new String(bytes.toByteArray, 0, startNonZeroCnt)
		} else {
			bytes.toHexString
		}
	}

	/*
	private var seContracts: Map[ByteArrayWrapper, StorageDictionary] = new HashMap[ByteArrayWrapper, StorageDictionary]

	def testDump(layout: StorageDictionaryDb.Layout): StorageDictionary = {
		val dict: StorageDictionary = new StorageDictionary
		import scala.collection.JavaConversions._
		for (key <- storeKeys.keySet) {
			dict.addPath(new DataWord(key.getData), getKeyOriginSolidity(key.getData))
		}
		dict
	}
			*/

	def dumpKeys(storage: ContractDetails) {
		//TODO dumpKeys: 未実装。
		/*
		val solidityDict: StorageDictionary = StorageDictionaryDb.INST.getOrCreate(StorageDictionaryDb.Layout.Solidity, contractAddress)
		val serpentDict: StorageDictionary = StorageDictionaryDb.INST.getOrCreate(StorageDictionaryDb.Layout.Serpent, contractAddress)
		import scala.collection.JavaConversions._
		for (key <- storeKeys.keySet) {
			solidityDict.addPath(new DataWord(key.getData), getKeyOriginSolidity(key.getData))
			serpentDict.addPath(new DataWord(key.getData), getKeyOriginSerpent(key.getData))
		}
		if (SystemProperties.CONFIG.getConfig.hasPath("vm.structured.storage.dictionary.dump")) {
			if (!solidityDict.isValid) {
				var f: File = new File("json")
				f.mkdirs
				f = new File(f, Hex.toHexString(contractAddress) + ".sol.txt")
				try {
					val w: BufferedWriter = new BufferedWriter(new FileWriter(f))
					val s: String = solidityDict.compactAndFilter(null).dump(storage)
					w.write(s)
					w.close
				}
				catch {
					case e: Exception => {
						e.printStackTrace
					}
				}
			}
			if (!serpentDict.isValid) {
				val f: File = new File("json", Hex.toHexString(contractAddress) + ".se.txt")
				f.getParentFile.mkdirs
				try {
					val w: BufferedWriter = new BufferedWriter(new FileWriter(f))
					val s: String = serpentDict.dump(storage)
					w.write(s)
					w.close
				}
				catch {
					case e: Exception => {
						e.printStackTrace
					}
				}
			}
			val f: File = new File("json", Hex.toHexString(contractAddress) + ".hash.txt")
			f.getParentFile.mkdirs
			try {
				val w: BufferedWriter = new BufferedWriter(new FileWriter(f, true))
				w.write("\nHashes:\n")
				import scala.collection.JavaConversions._
				for (entry <- hashes.values) {
					w.write(entry + "\n")
				}
				w.write("\nSSTORE:\n")
				import scala.collection.JavaConversions._
				for (entry <- storeKeys.entrySet) {
					w.write(entry + "\n")
				}
				w.close
			}
			catch {
				case e: Exception => {
					e.printStackTrace
				}
			}
		}
		StorageDictionaryDb.INST.put(StorageDictionaryDb.Layout.Solidity, contractAddress, solidityDict)
		StorageDictionaryDb.INST.put(StorageDictionaryDb.Layout.Serpent, contractAddress, serpentDict)
		*/
	}


	def vmStartPlayNotify(): Unit = {
		//
	}

	def vmEndPlayNotify(contractDetails: ContractDetails): Unit = {
		try {
			dumpKeys(contractDetails)
		} catch {
			case e: Throwable =>
				logger.error("Unexpected exception: ", e)
		}
	}

}

object StorageDictionaryHandler {
	private val logger: Logger = LoggerFactory.getLogger("vm")

	class Entry(val hashValue: VMWord, val input: ImmutableBytes) {
		override def toString: String = {
			"sha3(" + input.toHexString + ") = " + hashValue
		}
	}

	private def getMapKey(hash: ImmutableBytes): ImmutableBytes = {
		hash.copyOfRange(0, 20)
	}
}