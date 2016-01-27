package org.lipicalabs.lipica.core.net.p2p

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:10
 * YANAGISAWA, Kentaro
 */
sealed trait ReasonCode {
	def reason: Int
	def asByte = this.reason.asInstanceOf[Byte]
}

abstract class AbstractReasonCode(override val reason: Int) extends ReasonCode

object ReasonCode {

	private val map = new mutable.HashMap[Int, ReasonCode]
	private def register(reasonCode: ReasonCode) = this.map.put(reasonCode.reason, reasonCode)

	/** 相手から切断を要求された。 */
	case object Requested extends AbstractReasonCode(0x00)
	register(Requested)

	case object TcpError extends AbstractReasonCode(0x01)
	register(TcpError)

	/** 解析に失敗した。 */
	case object BadProtocol extends AbstractReasonCode(0x02)
	register(BadProtocol)

	/** 性能もしくは信頼性が低すぎる。 */
	case object UselessPeer extends AbstractReasonCode(0x03)
	register(UselessPeer)

	/** 既に多くの他のノードと接続している。 */
	case object TooManyPeers extends AbstractReasonCode(0x04)
	register(TooManyPeers)

	/** すでにこのノードと有効な接続を保持している。 */
	case object DuplicatePeer extends AbstractReasonCode(0x05)
	register(DuplicatePeer)

	/** プロトコル互換性がない。 */
	case object IncompatibleProtocol extends AbstractReasonCode(0x06)
	register(IncompatibleProtocol)

	case object NullIdentity extends AbstractReasonCode(0x07)
	register(NullIdentity)

	case object PeerQuiting extends AbstractReasonCode(0x08)
	register(PeerQuiting)

	case object ExpectedIdentity extends AbstractReasonCode(0x09)
	register(ExpectedIdentity)

	case object LocalIdentity extends AbstractReasonCode(0x0A)
	register(LocalIdentity)

	case object PingTimeout extends AbstractReasonCode(0x0B)
	register(PingTimeout)

	case object UserReason extends AbstractReasonCode(0x10)
	register(UserReason)

	case object Unknown extends AbstractReasonCode(0xFF)
	register(Unknown)

	val all = this.map.toMap
	private val intToTypeMap: scala.collection.immutable.Map[Int, ReasonCode] = all

	def fromInt(reason: Int): ReasonCode = this.intToTypeMap.getOrElse(reason, Unknown)

}