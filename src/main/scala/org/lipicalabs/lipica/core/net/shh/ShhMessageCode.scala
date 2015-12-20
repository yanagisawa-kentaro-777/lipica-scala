package org.lipicalabs.lipica.core.net.shh

import org.lipicalabs.lipica.core.net.message.Command

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 13:26
 * YANAGISAWA, Kentaro
 */
sealed trait ShhMessageCode extends Command {
	def command: Int
	def asByte: Byte = this.command.toByte
}

object ShhMessageCode {
	abstract class AbstractShhMessageCode(override val command: Int) extends ShhMessageCode

	private val map = new mutable.HashMap[Int, ShhMessageCode]
	private def register(code: ShhMessageCode): Unit = this.map.put(code.command, code)

	/**
	 * 現在の状態について通知する。
	 */
	case object Status extends AbstractShhMessageCode(0x00)
	register(Status)

	case object Message extends AbstractShhMessageCode(0x01)
	register(Message)

	case object AddFilter extends AbstractShhMessageCode(0x02)
	register(AddFilter)

	case object RemoveFilter extends AbstractShhMessageCode(0x03)
	register(RemoveFilter)

	case object PacketCount extends AbstractShhMessageCode(0x04)
	register(PacketCount)

	case object Unknown extends AbstractShhMessageCode(0xFF)

	val all = this.map.toMap

	def fromByte(b: Byte): ShhMessageCode = this.all.getOrElse(b & 0xFF, Unknown)

	def inRange(code: Byte): Boolean = (Status.command <= code) && (code <= PacketCount.command)

}