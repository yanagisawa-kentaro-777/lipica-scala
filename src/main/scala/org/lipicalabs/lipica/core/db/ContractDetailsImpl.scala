package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class ContractDetailsImpl(encodedBytes: ImmutableBytes) extends ContractDetails {

	def this() = this(null)
	//TODO 未実装。

	override def isDirty = ???

	override def getSnapshotTo(v: ImmutableBytes) = ???

	override def put(key: DataWord, value: DataWord) = ???

	override def getCode = ???

	override def getStorage(keys: Iterable[DataWord]) = ???

	def getStorageHash: ImmutableBytes = ???

	override def setCode(v: ImmutableBytes) = ???

	def get(key: DataWord): DataWord = ???

	override def setAddress(v: ImmutableBytes) = ???

	override def getStorageSize: Int = ???

	override def getStorage: Map[DataWord, DataWord] = ???

	override def syncStorage(): Unit = ???

	def getEncoded: ImmutableBytes = ???

	override def createClone: ContractDetails = ???
}
