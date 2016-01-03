package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db.datasource.{DataSourcePool, KeyValueDataSource}
import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class ContractDetailsImpl() extends ContractDetails {

	private var _address = ImmutableBytes.empty

	private var _code = ImmutableBytes.empty

	private val keys = new mutable.HashSet[ImmutableBytes]
	private var storageTrie = new SecureTrie(null)

	private var _isDirty = false
	private var _isDeleted = false
	private var externalStorage = false
	private var externalStorageDataSource: KeyValueDataSource = null

	override def address: ImmutableBytes = this._address
	override def address_=(v: ImmutableBytes) = this._address = v

	override def code = this._code
	override def code_=(v: ImmutableBytes) = this._code = v

	override def isDirty = this._isDirty
	override def isDirty_=(v: Boolean) = this._isDirty = v

	override def isDeleted: Boolean = this._isDeleted
	override def isDeleted_=(v: Boolean): Unit = this._isDeleted = v

	private def addKey(key: ImmutableBytes): Unit = this.keys.add(key)

	private def removeKey(key: ImmutableBytes): Unit = {
		//this.keys.remove(key)
	}

	def put(key: ImmutableBytes, value: ImmutableBytes): Unit = put(DataWord(key), DataWord(value))

	override def put(key: DataWord, value: DataWord) = {
		if (value == DataWord.Zero) {
			this.storageTrie.delete(key.data)
			removeKey(key.data)
		} else {
			val encodedValue = RBACCodec.Encoder.encode(value.getDataWithoutLeadingZeros)
			this.storageTrie.update(key.data, encodedValue)
			addKey(key.data)
		}
		this.isDirty = true
		this.externalStorage = externalStorage || (SystemProperties.CONFIG.detailsInMemoryStorageLimit < this.keys.size)
	}

	override def get(key: DataWord): Option[DataWord] = {
		val data = this.storageTrie.get(key.data)
		if (data.nonEmpty) {
			Some(DataWord(RBACCodec.Decoder.decode(data).right.get.bytes))
		} else {
			None
		}
	}

	override def storageRoot: ImmutableBytes = this.storageTrie.rootHash

	override def storageKeys: Set[DataWord] = this.keys.map(DataWord(_)).toSet

	override def put(data: Map[DataWord, DataWord]): Unit = {
		for (entry <- data) {
			put(entry._1, entry._2)
		}
	}

	override def storageContent(aKeys: Iterable[DataWord]) = {
		aKeys.flatMap {
			eachKey => {
				get(eachKey).map(v => (eachKey, v))
			}
		}.toMap
	}

	override def storageContent: Map[DataWord, DataWord] = {
		this.keys.flatMap {
			each => {
				val eachKey = DataWord(each)
				get(eachKey).map(v => (eachKey, v))
			}
		}.toMap
	}

	override def storageSize: Int = this.keys.size

	override def syncStorage(): Unit = {
		if (externalStorage) {
			this.storageTrie.cache.setDB(getExternalStorageDataSource)
			this.storageTrie.sync()

			DataSourcePool.closeDataSource(dataSourceName)
		}
	}

	private def dataSourceName = "details-storage/" + address.toHexString

	private def getExternalStorageDataSource: KeyValueDataSource = {
		if (this.externalStorageDataSource eq null) {
			this.externalStorageDataSource = DataSourcePool.levelDbByName(dataSourceName)
		}
		this.externalStorageDataSource
	}
	def setExternalStorageDataSource(v: KeyValueDataSource): Unit = {
		this.externalStorageDataSource = v
	}

	override def getSnapshotTo(hash: ImmutableBytes) = {
		val keyValueDataSource = this.storageTrie.cache.dataSource
		val snapStorage =
			if (hash == DigestUtils.EmptyTrieHash) {
				new SecureTrie(keyValueDataSource)
			} else {
				new SecureTrie(keyValueDataSource, hash)
			}
		snapStorage.cache = this.storageTrie.cache

		val details = ContractDetailsImpl.newInstance(this.address, snapStorage, this.code)
		this.keys.foreach(details.keys.add)
		details
	}

	override def createClone: ContractDetails = {
		val result = new ContractDetailsImpl
		result.address = this.address
		result.code = this.code
		this.storageContent.foreach {
			entry => result.put(entry._1, entry._2)
		}
		result
	}

	override def encode: ImmutableBytes = {
		val encodedAddress = RBACCodec.Encoder.encode(this.address)
		val encodedIsExternalStorage = RBACCodec.Encoder.encode(this.externalStorage)
		val encodedStorageRoot = RBACCodec.Encoder.encode(
			if (this.externalStorage) {
				this.storageTrie.rootHash
			} else {
				Array.emptyByteArray
			}
		)
		val encodedStorage = RBACCodec.Encoder.encode(this.storageTrie.serialize)
		val encodedCode = RBACCodec.Encoder.encode(this.code)
		val encodedKeys = RBACCodec.Encoder.encode(this.keys.toSeq)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, encodedIsExternalStorage, encodedStorage, encodedCode, encodedKeys, encodedStorageRoot))
	}

	override def decode(data: ImmutableBytes) = {
		val items = RBACCodec.Decoder.decode(data).right.get.items
		this.address = items.head.bytes
		this.externalStorage = items(1).asPositiveLong > 0L
		this.storageTrie.deserialize(items(2).bytes)
		this.code = items(3).bytes
		items(4).items.foreach {
			each => addKey(each.bytes)
		}
		if (externalStorage) {
			this.storageTrie.root = items(5).bytes
			this.storageTrie.cache.setDB(getExternalStorageDataSource)
		}
	}

	override def toString: String = {
		"Code: %s; Storage: %s".format(this.code.toHexString, storageContent.toString())
	}

}

object ContractDetailsImpl {

	def decode(bytes: ImmutableBytes): ContractDetailsImpl = {
		val result = new ContractDetailsImpl
		result.decode(bytes)
		result
	}

	def newInstance(address: ImmutableBytes, trie: SecureTrie, code: ImmutableBytes): ContractDetailsImpl = {
		val result = new ContractDetailsImpl
		result.address = address
		result.storageTrie = trie
		result.code = code
		result
	}

}