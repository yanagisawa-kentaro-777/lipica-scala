package org.lipicalabs.lipica.core.kernel

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/09 21:41
 * YANAGISAWA, Kentaro
 */
class ContractDetailsCacheImpl(private[core] var originalContract: ContractDetails) extends ContractDetails {

	private val storageRef = new AtomicReference[mutable.Map[DataWord, DataWord]](new mutable.HashMap[DataWord, DataWord])
	private def storage: mutable.Map[DataWord, DataWord] = this.storageRef.get

	private val codeRef = new AtomicReference[ImmutableBytes](
		Option(this.originalContract).map(_.code).getOrElse(ImmutableBytes.empty)
	)
	override def code = this.codeRef.get
	override def code_=(v: ImmutableBytes) = this.codeRef.set(v)

	private val isDirtyRef = new AtomicBoolean(false)
	override def isDirty = this.isDirtyRef.get
	override def isDirty_=(v: Boolean) = this.isDirtyRef.set(v)

	private val isDeletedRef = new AtomicBoolean(false)
	override def isDeleted: Boolean = this.isDeletedRef.get
	override def isDeleted_=(v: Boolean): Unit = this.isDeletedRef.set(v)

	override def put(key: DataWord, value: DataWord) = {
		this.synchronized {
			this.storage.put(key, value)
			this.isDirty = true
		}
	}

	override def get(key: DataWord) = {
		this.synchronized {
			val value = this.storage.getOrElse(key,
				if (this.originalContract eq null) {
					DataWord.Zero
				} else {
					val v = this.originalContract.get(key).getOrElse(DataWord.Zero)
					this.storage.put(key, v)
					v
				}
			)
			if (value.isZero) {
				None
			} else {
				Some(value)
			}
		}
	}

	override def storageRoot: ImmutableBytes = {
		this.synchronized {
			val storageTrie = SecureTrie.newInstance
			for (entry <- this.storage) {
				val (key, value) = entry
				val encodedValue = RBACCodec.Encoder.encode(value.getDataWithoutLeadingZeros)
				storageTrie.update(key.data, encodedValue)
			}
			storageTrie.rootHash
		}
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
		this.synchronized {
			val result = new ContractDetailsCacheImpl(this.originalContract)
			val storageClone = this.storage.clone()

			result.code = this.code
			result.storageRef.set(storageClone)

			result
		}
	}

	override def encode = {
		this.synchronized {
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
	}

	override def decode(data: ImmutableBytes) = {
		this.synchronized {
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
	}

	override def syncStorage() = Option(this.originalContract).foreach(_.syncStorage())

	override def getSnapshotTo(v: ImmutableBytes) = throw new UnsupportedOperationException

	override def toString: String = "Code: %s, StorageSize: %,d".format(this.code.toHexString, this.storageSize)

	def commit(): Unit = {
		this.synchronized {
			if (this.originalContract eq null) return

			for (entry <- this.storage) {
				originalContract.put(entry._1, entry._2)
			}
			originalContract.code = this.code
			originalContract.isDirty = this.isDirty || originalContract.isDirty
		}
	}

}
