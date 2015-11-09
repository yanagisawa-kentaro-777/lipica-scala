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

	def getStorageHash: ImmutableBytes = ???

	override def isDirty = ???
	override def get(key: DataWord) = ???

	override def getEncoded = ???

	override def getStorageSize = ???

	override def put(key: DataWord, value: DataWord) = ???

	override def getCode = ???

	override def syncStorage() = ???

	override def getSnapshotTo(v: ImmutableBytes) = ???

	override def getStorage(keys: Iterable[DataWord]) = ???

	override def getStorage = ???

	override def setCode(v: ImmutableBytes) = ???

	override def setAddress(v: ImmutableBytes) = ???
}
