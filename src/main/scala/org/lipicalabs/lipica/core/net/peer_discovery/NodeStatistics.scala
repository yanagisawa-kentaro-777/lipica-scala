package org.lipicalabs.lipica.core.net.peer_discovery

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.p2p.ReasonCode
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}

/**
 * 自ノードの、外部ノードとの通信に関する統計情報を保持するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:51
 * YANAGISAWA, Kentaro
 */
class NodeStatistics(val node: Node) {

	import NodeStatistics._

	private val isPredefinedRef: AtomicBoolean = new AtomicBoolean(false)
	def isPredefined: Boolean = this.isPredefinedRef.get
	def setPredefined(v: Boolean): Unit = this.isPredefinedRef.set(true)

	private val savedReputationRef = new AtomicInteger(0)
	private def savedReputation: Int = this.savedReputationRef.get

	def getPersistentData: NodeStatistics.Persistent = {
		val result = new Persistent
		result.reputation = getSessionFairReputation + (this.savedReputation / 2)
		result
	}
	def setPersistedData(persistent: NodeStatistics.Persistent): Unit = this.savedReputationRef.set(persistent.reputation)

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

	private val clientIdRef: AtomicReference[String] = new AtomicReference[String]("")
	def clientId: String = this.clientIdRef.get
	def clientId_=(v: String): Unit = this.clientIdRef.set(v)

	private val transportLastRemoteDisconnectReasonRef: AtomicReference[ReasonCode] = new AtomicReference[ReasonCode](null)
	private def transportLastRemoteDisconnectReason: ReasonCode = this.transportLastRemoteDisconnectReasonRef.get

	private val transportLastLocalDisconnectReasonRef: AtomicReference[ReasonCode] = new AtomicReference[ReasonCode](null)
	private def transportLastLocalDisconnectReason: ReasonCode = transportLastLocalDisconnectReasonRef.get

	private val disconnectedRef: AtomicBoolean = new AtomicBoolean(false)
	private def isDisconnected: Boolean = this.disconnectedRef.get

	val lpcHandshake = new StatHandler
	val lpcInbound = new StatHandler
	val lpcOutbound = new StatHandler

	private val lpcLastInboundStatusMessageRef: AtomicReference[StatusMessage] = new AtomicReference[StatusMessage](null)
	def lpcLastInboundStatusMessage: StatusMessage = this.lpcLastInboundStatusMessageRef.get
	def lpcLastInboundStatusMessage_=(v: StatusMessage): Unit = this.lpcLastInboundStatusMessageRef.set(v)

	private val lpcTotalDifficultyRef: AtomicReference[BigInt] = new AtomicReference[BigInt](UtilConsts.Zero)
	def lpcTotalDifficulty_=(v: BigInt): Unit = this.lpcTotalDifficultyRef.set(v)
	def lpcTotalDifficulty: BigInt = this.lpcTotalDifficultyRef.get


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

		if (this.isDisconnected) {
			if ((this.transportLastLocalDisconnectReason eq null) && (this.transportLastRemoteDisconnectReason eq null)) {
				//無理由切断。ペナルティ。
				transportReputation = (transportReputation * 0.3d).toInt
			} else if (this.transportLastLocalDisconnectReason != ReasonCode.Requested) {
				if (this.transportLastRemoteDisconnectReason == ReasonCode.TooManyPeers) {
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

	def reputation: Int = (this.savedReputation / 2) + getSessionReputation

	def nodeDisconnectedRemote(reason: ReasonCode): Unit = this.transportLastRemoteDisconnectReasonRef.set(reason)

	def nodeDisconnectedLocal(reason: ReasonCode): Unit = this.transportLastLocalDisconnectReasonRef.set(reason)

	def disconnected(): Unit = this.disconnectedRef.set(true)

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
		private val reputationRef = new AtomicInteger(0)
		def reputation: Int = this.reputationRef.get
		def reputation_=(v: Int): Unit = this.reputationRef.set(v)

		def encode: ImmutableBytes = RBACCodec.Encoder.encode(this.reputation)
	}

	object Persistent {
		def decode(encodedBytes: ImmutableBytes): Persistent = {
			val value = RBACCodec.Decoder.decode(encodedBytes).right.get.asInt
			val result = new Persistent
			result.reputationRef.set(value)
			result
		}
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