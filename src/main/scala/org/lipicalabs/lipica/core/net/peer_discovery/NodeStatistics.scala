package org.lipicalabs.lipica.core.net.peer_discovery

import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}

import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.message.ReasonCode
import org.lipicalabs.lipica.core.utils.UtilConsts

/**
 * 自ノードの、外部ノードとの通信に関する統計情報を保持するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:51
 * YANAGISAWA, Kentaro
 */
class NodeStatistics(val node: Node) {

	import NodeStatistics._

	private var _isPredefined: Boolean = false
	def isPredefined: Boolean = this._isPredefined
	def setPredefined(v: Boolean): Unit = this._isPredefined = true

	private var _savedReputation = 0

	def getPersistentData: NodeStatistics.Persistent = {
		val result = new Persistent
		result.reputation = getSessionFairReputation + (this._savedReputation / 2)
		result
	}
	def setPersistedData(persistent: NodeStatistics.Persistent): Unit = this._savedReputation = persistent.reputation

	val discoverInPing = new StatHandler
	val discoverInPong = new StatHandler
	val discoverInNeighbours = new StatHandler
	val discoverInFind = new StatHandler

	val discoverOutPing = new StatHandler
	val discoverOutPong = new StatHandler
	val discoverOutNeighbours = new StatHandler
	val discoverOutFind = new StatHandler

	val transportConnectionAttempts = new StatHandler
	val transportAuthMessageSent = new StatHandler
	val transportOutHello = new StatHandler
	val transportInHello = new StatHandler
	val transportHandshake = new StatHandler
	val transportOutMessages = new StatHandler
	val transportInMessages = new StatHandler

	def statName = "discover.nodes.%s".format(this.node.address)
	val discoverMessageLatency: Statter = new SimpleStatter(statName + ".message.latency")

	private var _clientId: String = ""
	def clientId: String = this._clientId
	def clientId_=(v: String): Unit = this._clientId = v

	private var _transportLastRemoteDisconnectReason: ReasonCode = null
	private var _transportLastLocalDisconnectReason: ReasonCode = null

	private var _disconnected: Boolean = false

	val lpcHandshake = new StatHandler
	val lpcInbound = new StatHandler
	val lpcOutbound = new StatHandler

	private var _lpcLastInboundStatusMessage: StatusMessage = null
	def lpcLastInboundStatusMessage: StatusMessage = this._lpcLastInboundStatusMessage
	def lpcLastInboundStatusMessage_=(v: StatusMessage): Unit = this._lpcLastInboundStatusMessage = v

	private var _lpcTotalDifficulty: BigInt = UtilConsts.Zero
	def lpcTotalDifficulty_=(v: BigInt): Unit = this._lpcTotalDifficulty = v
	def lpcTotalDifficulty: BigInt = this._lpcTotalDifficulty


	def getSessionReputation: Int = {
		getSessionFairReputation + (if (this.isPredefined) ReputationPredefined else 0)
	}

	def getSessionFairReputation: Int = {
		var discoverReputation = 0
		discoverReputation += (discoverInPong.get min 10) * (if (discoverOutPing.get == discoverInPong.get) 2 else 1)
		discoverReputation += (discoverInNeighbours.get min 10) * 2

		var transportReputation: Int = 0
		transportReputation += (if (transportAuthMessageSent.get > 0) 10 else 0)
		transportReputation += (if (transportHandshake.get > 0) 20 else 0)
		transportReputation += (transportInMessages.get min 10) * 3

		if (this._disconnected) {
			if ((this._transportLastLocalDisconnectReason eq null) && (this._transportLastRemoteDisconnectReason eq null)) {
				//無理由切断。ペナルティ。
				transportReputation = (transportReputation * 0.3d).toInt
			} else if (this._transportLastLocalDisconnectReason != ReasonCode.Requested) {
				if (this._transportLastRemoteDisconnectReason == ReasonCode.TooManyPeers) {
					//このピアは人気がある。自ノードが不運であった。
					(transportReputation * 0.8d).toInt
				} else {
					//別の理由。
					(transportReputation * 0.5d).toInt
				}
			}
		}
		discoverReputation + 100 * transportReputation
	}

	def reputation: Int = (this._savedReputation / 2) + getSessionReputation

	def nodeDisconnectedRemote(reason: ReasonCode): Unit = this._transportLastRemoteDisconnectReason = reason

	def nodeDisconnectedLocal(reason: ReasonCode): Unit = this._transportLastLocalDisconnectReason = reason

	def disconnected(): Unit = this._disconnected = true

	def lpcHandshake(message: StatusMessage): Unit = {
		this.lpcLastInboundStatusMessage = message
		this.lpcTotalDifficulty_=(message.totalDifficulty.toPositiveBigInt)
		this.lpcHandshake.add
	}

	override def toString: String = {
		//TODO 未実装。
		"NodeStatistics"
	}

}

object NodeStatistics {

	val ReputationPredefined: Int = 1000500

	class Persistent extends Serializable {
		var reputation: Int = 0
	}
}

class StatHandler {
	val count = new AtomicInteger(0)
	def add: Int = this.count.incrementAndGet
	def get: Int = this.count.get
	override def toString = this.count.toString
}

trait Statter {
	def add(v: Double): Unit
}

class SimpleStatter(val name: String) extends Statter {

	private val lastRef = new AtomicReference[Double](0d)
	def last: Double = this.lastRef.get

	private val sumRef = new AtomicReference[Double] (0d)
	def sum: Double = this.sumRef.get

	private val countRef = new AtomicInteger(0)
	def count: Int = this.countRef.get

	def average: Double = {
		val c = this.count
		if (c == 0) {
			return 0d
		}
		this.sum / c.toDouble
	}

	override def add(value: Double): Unit = {
		this.lastRef.set(value)
		this.sumRef.set(this.sumRef.get + value)
		this.countRef.incrementAndGet
	}
}