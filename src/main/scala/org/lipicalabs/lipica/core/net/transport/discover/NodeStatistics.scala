package org.lipicalabs.lipica.core.net.transport.discover

import java.util.concurrent.atomic.AtomicInteger

import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.ReasonCode
import org.lipicalabs.lipica.core.utils.UtilConsts

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:51
 * YANAGISAWA, Kentaro
 */
class NodeStatistics {

	//TODO 未実装。

	def clientId: String = ???
	def clientId_=(v: String): Unit = ???

	private var _lpcTotalDifficulty: BigInt = UtilConsts.Zero
	def lpcTotalDifficulty_=(v: BigInt): Unit = this._lpcTotalDifficulty = v
	def lpcTotalDifficulty: BigInt = this._lpcTotalDifficulty

	val lpcInbound = new StatHandler
	val lpcOutbound = new StatHandler

	def lpcHandshake(message: StatusMessage): Unit = ???

	def nodeDisconnectedLocal(reason: ReasonCode): Unit = ???

	def nodeDisconnectedRemote(reason: ReasonCode): Unit = ???

}

class StatHandler {
	val count = new AtomicInteger(0)
	def add: Int = this.count.incrementAndGet
	def get: Int = this.count.get
	override def toString = this.count.toString
}