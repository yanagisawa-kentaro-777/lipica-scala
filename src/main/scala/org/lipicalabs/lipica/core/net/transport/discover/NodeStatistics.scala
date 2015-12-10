package org.lipicalabs.lipica.core.net.transport.discover

import java.util.concurrent.atomic.AtomicInteger

import org.lipicalabs.lipica.core.net.message.ReasonCode

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:51
 * YANAGISAWA, Kentaro
 */
class NodeStatistics {

	//TODO 未実装。

	val lpcOutbound = new StatHandler

	def nodeDisconnectedLocal(reason: ReasonCode): Unit = ???

	def nodeDisconnectedRemote(reason: ReasonCode): Unit = ???

}

class StatHandler {
	val count = new AtomicInteger(0)
	def add(): Int = this.count.incrementAndGet
	def get: Int = this.count.get
	override def toString = this.count.toString
}