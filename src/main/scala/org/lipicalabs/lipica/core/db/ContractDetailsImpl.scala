package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class ContractDetailsImpl(encodedBytes: ImmutableBytes) extends ContractDetails {
	//TODO 未実装。
	override def setAddress(v: ImmutableBytes) = ???

	override def getStorageSize: Int = ???

	override def getStorage: Map[DataWord, DataWord] = ???

	override def syncStorage(): Unit = ???

	def getEncoded: ImmutableBytes = ???
}
