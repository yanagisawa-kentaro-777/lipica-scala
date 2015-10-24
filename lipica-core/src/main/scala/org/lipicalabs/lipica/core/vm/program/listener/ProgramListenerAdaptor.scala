package org.lipicalabs.lipica.core.vm.program.listener

import org.lipicalabs.lipica.core.vm.DataWord

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class ProgramListenerAdaptor extends ProgramListener {

	override def onMemoryExtend(delta: Int): Unit = ()

	override def onMemoryWrite(address: Int, data: Array[Byte], size: Int): Unit = ()

	override def onStorageClear(): Unit = ()

	override def onStackPop(): Unit = ()

	override def onStoragePut(key: DataWord, value: DataWord): Unit = ()

	override def onStackSwap(from: Int, to: Int): Unit = ()

	override def onStackPush(value: DataWord): Unit = ()

}
