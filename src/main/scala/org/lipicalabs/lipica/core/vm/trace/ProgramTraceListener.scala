package org.lipicalabs.lipica.core.vm.trace

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord
import org.lipicalabs.lipica.core.vm.program.listener.ProgramListenerAdaptor

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 13:48
 * YANAGISAWA, Kentaro
 */
class ProgramTraceListener extends ProgramListenerAdaptor {

	private val enabled: Boolean = NodeProperties.CONFIG.vmTrace
	private var actions = OpActions.create

	override def onMemoryExtend(delta: Int): Unit = {
		if (this.enabled) {
			this.actions = this.actions.memoryExtend(delta)
		}
	}

	override def onMemoryWrite(address: Int, data: ImmutableBytes, size: Int): Unit = {
		if (this.enabled) {
			this.actions = this.actions.memoryWrite(address, data, size)
		}
	}

	override def onStackPop(): Unit = {
		if (this.enabled) {
			this.actions = this.actions.stackPop
		}
	}

	override def onStackPush(value: VMWord): Unit = {
		if (this.enabled) {
			this.actions = this.actions.stackPush(value)
		}
	}

	override def onStackSwap(from: Int, to: Int): Unit = {
		if (this.enabled) {
			this.actions = this.actions.stackSwap(from, to)
		}
	}

	override def onStoragePut(key: VMWord, value: VMWord): Unit = {
		if (this.enabled) {
			if (value == VMWord.Zero) {
				this.actions = this.actions.storageRemove(key)
			} else {
				this.actions = this.actions.storagePut(key, value)
			}
		}
	}

	override def onStorageClear(): Unit = {
		if (this.enabled) {
			this.actions = this.actions.storageClear()
		}
	}

	def resetActions(): OpActions = {
		val result = this.actions
		this.actions = OpActions.create
		result
	}
}
