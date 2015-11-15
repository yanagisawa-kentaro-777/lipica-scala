package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/11/09 21:41
 * YANAGISAWA, Kentaro
 */
class ContractDetailsCacheImpl(details: ContractDetails) extends ContractDetails {

	var originalContract: ContractDetails = ???

	def commit(): Unit = ???

	override def storageHash: ImmutableBytes = ???

	override def storageSize = ???

	override def storageContent = ???

	override def storageContent(keys: Iterable[DataWord]) = ???

	override def storageKeys: Set[DataWord] = ???

	override def get(key: DataWord) = ???

	override def encode = ???

	override def decode(data: ImmutableBytes) = ???

	override def put(key: DataWord, value: DataWord) = ???

	override def put(data: Map[DataWord, DataWord]): Unit = ???

	override def syncStorage() = ???

	override def getSnapshotTo(v: ImmutableBytes) = ???

	override def code = ???
	override def code_=(v: ImmutableBytes) = ???

	override def address_=(v: ImmutableBytes) = ???
	override def address: ImmutableBytes = ???

	override def isDeleted: Boolean = ???
	override def isDeleted_=(v: Boolean): Unit = ???

	override def isDirty = ???
	override def isDirty_=(v: Boolean) = ???

	override def createClone: ContractDetails = ???

}
