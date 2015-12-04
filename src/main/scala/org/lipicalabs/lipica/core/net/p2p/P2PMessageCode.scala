package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.net.message.Command

import scala.collection.mutable

sealed trait P2PMessageCode extends Command {
	def command: Int
	def asByte: Byte = this.command.toByte
}


/**
 * コマンドの集合。
 *
 *
 * Created by IntelliJ IDEA.
 * 2015/12/04 20:53
 * YANAGISAWA, Kentaro
 */
object P2PMessageCode {

	private val map = new mutable.HashMap[Int, P2PMessageCode]

	abstract class AbstractP2PMessageCode(override val command: Int) extends P2PMessageCode

	private def register(code: P2PMessageCode): Unit = this.map.put(code.command, code)

	/**
	 * 接続上で最初に送信されるメッセージ。
	 * 両ノードから一度ずつ送信される。
	 */
	case object Hello extends AbstractP2PMessageCode(0x00)
	register(Hello)

	/**
	 * このメッセージを受信したら、即座に接続を切断すべきである。
	 */
	case object Disconnect extends AbstractP2PMessageCode(0x01)
	register(Disconnect)

	/**
	 * Pongによって返信することを要求するためのメッセージ。
	 */
	case object Ping extends AbstractP2PMessageCode(0x02)
	register(Ping)

	/**
	 * 相手からのPingに対する返信メッセージ。
	 */
	case object Pong extends AbstractP2PMessageCode(0x03)
	register(Pong)

	/**
	 * 知っているピアをいくつか情報提供するように要求するメッセージ。
	 */
	case object GetPeers extends AbstractP2PMessageCode(0x04)
	register(GetPeers)

	/**
	 * 既知のノード情報を提供するためのメッセージ。
	 */
	case object Peers extends AbstractP2PMessageCode(0x05)
	register(Peers)

	case object User extends AbstractP2PMessageCode(0x0F)
	register(User)

	case object Unknown extends AbstractP2PMessageCode(0xFF)

	private val all = this.map.toMap

	def fromByte(b: Byte): P2PMessageCode = this.all.getOrElse(b & 0xFF, Unknown)

	def inRange(b: Byte): Boolean = (Hello.asByte <= b) && (b <= User.asByte)

}
