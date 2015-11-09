package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:03
 * YANAGISAWA, Kentaro
 */
trait ContractDetails {
	//TODO 未実装。
	//TODO これを実装したら、Storageにも実装を保管すること。

	def isDirty: Boolean

	def get(key: DataWord): DataWord

	def put(key: DataWord, value: DataWord): Unit

	def getStorageSize: Int

	def getStorage: Map[DataWord, DataWord]

	def getStorageHash: ImmutableBytes

	def getStorage(keys: Iterable[DataWord]): Map[DataWord, DataWord]

	def setAddress(v: ImmutableBytes): Unit

	def syncStorage(): Unit

	def getEncoded: ImmutableBytes

	def getCode: ImmutableBytes

	def setCode(v: ImmutableBytes): Unit

	def getSnapshotTo(v: ImmutableBytes): ContractDetails

}
