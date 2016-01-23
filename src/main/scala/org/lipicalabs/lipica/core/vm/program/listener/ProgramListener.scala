package org.lipicalabs.lipica.core.vm.program.listener

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
trait ProgramListener {

	def onMemoryExtend(delta: Int): Unit

	def onMemoryWrite(address: Int, data: ImmutableBytes, size: Int): Unit

	def onStackPop(): Unit

	def onStackPush(value: VMWord): Unit

	def onStackSwap(from: Int, to: Int): Unit

	def onStoragePut(key: VMWord, value: VMWord): Unit

	def onStorageClear(): Unit

}
