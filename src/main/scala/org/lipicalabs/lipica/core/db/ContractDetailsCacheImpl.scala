package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/09 21:41
 * YANAGISAWA, Kentaro
 */
class ContractDetailsCacheImpl(private[db] var originalContract: ContractDetails) extends ContractDetails {

	private var storage = new mutable.HashMap[DataWord, DataWord]

	private var _code = ImmutableBytes.empty
	override def code = this._code
	override def code_=(v: ImmutableBytes) = this._code = v

	private var _isDirty = false
	private var _isDeleted = false

	override def isDirty = this._isDirty
	override def isDirty_=(v: Boolean) = this._isDirty = v

	override def isDeleted: Boolean = this._isDeleted
	override def isDeleted_=(v: Boolean): Unit = this._isDeleted = v

	override def put(key: DataWord, value: DataWord) = {
		this.storage.put(key, value)
		this.isDirty = true
	}

	override def get(key: DataWord) = {
		val value = this.storage.getOrElse(key,
			if (this.originalContract eq null) {
				DataWord.Zero
			} else {
				val v = this.originalContract.get(key).getOrElse(DataWord.Zero)
				this.storage.put(key , v)
				v
			}
		)
		if  (value.isZero) {
			None
		} else {
			Some(value)
		}
	}

	override def storageHash: ImmutableBytes = {
		val storageTrie = new SecureTrie(null)
		for (entry <- this.storage) {
			val (key, value) = entry
			val encodedValue = RBACCodec.Encoder.encode(value.getDataWithoutLeadingZeros)
			storageTrie.update(key.data, encodedValue)
		}
		storageTrie.rootHash
	}

	override def storageContent = this.storage.toMap

	override def storageContent(aKeys: Iterable[DataWord]) = {
		aKeys.flatMap {
			eachKey => {
				this.storage.get(eachKey).map(v => (eachKey, v))
			}
		}.toMap
	}

	override def storageKeys: Set[DataWord] = {
		if (originalContract eq null) {
			this.storage.keySet.toSet
		} else {
			this.originalContract.storageKeys
		}
	}

	override def storageSize = {
		if (originalContract eq null) {
			this.storage.size
		} else {
			this.originalContract.storageSize
		}
	}

	override def put(data: Map[DataWord, DataWord]): Unit = {
		for (entry <- data) {
			this.storage.put(entry._1, entry._2)
		}
	}

	override def address: ImmutableBytes = Option(this.originalContract).map(_.address).getOrElse(ImmutableBytes.empty)

	override def address_=(v: ImmutableBytes) = Option(this.originalContract).foreach(_.address = v)

	override def createClone: ContractDetails = {
		val result = new ContractDetailsCacheImpl(this.originalContract)
		val storageClone = this.storage.clone()

		result.code = this.code
		result.storage = storageClone

		result
	}

	override def encode = {
		val tupleSeq = this.storage.map {
			entry => {
				val encodedKey = RBACCodec.Encoder.encode(entry._1)
				val encodedValue = RBACCodec.Encoder.encode(entry._2.getDataWithoutLeadingZeros)
				(encodedKey, encodedValue)
			}
		}.toSeq
		val encodedKeySeq = RBACCodec.Encoder.encodeSeqOfByteArrays(tupleSeq.map(_._1))
		val encodedValueSeq = RBACCodec.Encoder.encodeSeqOfByteArrays(tupleSeq.map(_._2))
		val encodedCode = RBACCodec.Encoder.encode(this.code)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedKeySeq, encodedValueSeq, encodedCode))
	}

	override def decode(data: ImmutableBytes) = {
		val decodedItems = RBACCodec.Decoder.decode(data).right.get.items
		val decodedKeySeq = decodedItems.head.items
		val decodedValueSeq = decodedItems(1).items
		val decodedCode = decodedItems(2).bytes

		decodedKeySeq.indices.foreach {
			i => {
				this.storage.put(DataWord(decodedKeySeq(i).bytes), DataWord(decodedValueSeq(i).bytes))
			}
		}
		this.code = decodedCode
	}

	override def syncStorage() = Option(this.originalContract).foreach(_.syncStorage())

	override def getSnapshotTo(v: ImmutableBytes) = throw new UnsupportedOperationException

	override def toString: String = "Code: %s, Storage: %s".format(this.code.toHexString, this.storageContent.toString())

	def commit(): Unit = {
		if (this.originalContract eq null) return

		for (entry <- this.storage) {
			originalContract.put(entry._1, entry._2)
		}
		originalContract.code = this.code
		originalContract.isDirty = this.isDirty || originalContract.isDirty
	}

}
