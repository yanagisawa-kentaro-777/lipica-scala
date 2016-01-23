package org.lipicalabs.lipica.core.vm.trace

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord
import org.lipicalabs.lipica.core.vm.trace.OpActions._

import scala.collection.immutable.Queue

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 14:54
 * YANAGISAWA, Kentaro
 */
case class OpActions(private val stack: Queue[Action], private val memory: Queue[Action], private val storage: Queue[Action]) {

	def stackPop: OpActions = {
		new OpActions(this.stack :+ Action(Pop), this.memory, this.storage)
	}

	def stackPush(value: VMWord): OpActions = {
		val action = Action(Push).addParam("value", value)
		new OpActions(this.stack :+ action, this.memory, this.storage)
	}

	def stackSwap(from: Int, to: Int): OpActions = {
		val action = Action(Swap).addParam("from", from).addParam("to", to)
		new OpActions(this.stack :+ action, this.memory, this.storage)
	}

	def memoryExtend(delta: Long): OpActions = {
		val action = Action(Extend).addParam("delta", delta)
		new OpActions(this.stack, this.memory :+ action, this.storage)
	}

	def memoryWrite(address: Int, data: ImmutableBytes, size: Int): OpActions = {
		val action = Action(Write).addParam("address", address).addParam("data", data.toHexString.substring(0, size * 2))
		new OpActions(this.stack, this.memory :+ action, this.storage)
	}

	def storagePut(key: VMWord, value: VMWord): OpActions = {
		val action = Action(Put).addParam("key", key).addParam("value", value)
		new OpActions(this.stack, this.memory, this.storage :+ action)
	}

	def storageRemove(key: VMWord): OpActions = {
		val action = Action(Remove).addParam("key", key)
		new OpActions(this.stack, this.memory, this.storage :+ action)
	}

	def storageClear(): OpActions = {
		val action = Action(Clear)
		new OpActions(this.stack, this.memory, this.storage :+ action)
	}
}

object OpActions {

	sealed trait OpName
	case object Pop extends OpName
	case object Push extends OpName
	case object Swap extends OpName
	case object Extend extends OpName
	case object Write extends OpName
	case object Put extends OpName
	case object Remove extends OpName
	case object Clear extends OpName

	def create: OpActions = new OpActions(Queue.empty, Queue.empty, Queue.empty)

	case class Action(name: OpName, params: Map[String, Any]) {
		def addParam(key: String, value: Any): Action = {
			val newParams = this.params + (key -> value)
			Action(this.name, newParams)
		}
	}

	object Action {
		def apply(name: OpName): Action = new Action(name, Map.empty)
	}

}