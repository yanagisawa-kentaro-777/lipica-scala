package org.lipicalabs.lipica.core.vm.program.listener

import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 14:37
 * YANAGISAWA, Kentaro
 */
class CompositeProgramListener extends ProgramListener {

	private var listeners: Seq[ProgramListener]  = Seq.empty

	override def onMemoryExtend(delta: Int): Unit = this.listeners.foreach(_.onMemoryExtend(delta))

	override def onMemoryWrite(address: Int, data: Array[Byte], size: Int) = this.listeners.foreach(_.onMemoryWrite(address, data, size))

	override def onStoragePut(key: DataWord, value: DataWord) = this.listeners.foreach(_.onStoragePut(key, value))

	override def onStorageClear() = this.listeners.foreach(_.onStorageClear())

	override def onStackPush(value: DataWord) = this.listeners.foreach(_.onStackPush(value))

	override def onStackPop() = this.listeners.foreach(_.onStackPop())

	override def onStackSwap(from: Int, to: Int) = this.listeners.foreach(_.onStackSwap(from, to))

	def isEmpty: Boolean = this.listeners.isEmpty

	def addListener(listener: ProgramListener): Unit = {
		this.listeners = this.listeners :+ listener
	}
}

object CompositeProgramListener {
	def apply(listeners: Iterable[ProgramListener]): CompositeProgramListener = {
		val result = new CompositeProgramListener
		listeners.foreach(each => result.addListener(each))
		result
	}
}