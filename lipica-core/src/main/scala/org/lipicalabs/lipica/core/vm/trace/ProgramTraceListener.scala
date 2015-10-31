package org.lipicalabs.lipica.core.vm.trace

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.listener.ProgramListenerAdaptor

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 13:48
 * YANAGISAWA, Kentaro
 */
class ProgramTraceListener extends ProgramListenerAdaptor {

	private val enabled: Boolean = SystemProperties.CONFIG.vmTrace
	//private val actions = new OpActions

	override def onMemoryExtend(delta: Int): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onMemoryWrite(address: Int, data: Array[Byte], size: Int): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onStackPop(): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onStackPush(value: DataWord): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onStackSwap(from: Int, to: Int): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onStoragePut(key: DataWord, value: DataWord): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	override def onStorageClear(): Unit = {
		if (this.enabled) {
			//TODO
		}
	}

	def resetActions(): Unit = {
		//TODO
	}
}
