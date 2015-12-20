package org.lipicalabs.lipica.core.net.swarm.bzz

import org.lipicalabs.lipica.core.net.message.Command

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 13:26
 * YANAGISAWA, Kentaro
 */
sealed trait BzzMessageCode extends Command {
	def command: Int
	def asByte: Byte = this.command.toByte
}

object BzzMessageCode {
	abstract class AbstractBzzMessageCode(override val command: Int) extends BzzMessageCode

	private val map = new mutable.HashMap[Int, BzzMessageCode]
	private def register(code: BzzMessageCode): Unit = this.map.put(code.command, code)

	/**
	 * 現在の状態について通知する。
	 */
	case object Status extends AbstractBzzMessageCode(0x00)
	register(Status)

	case object StoreRequest extends AbstractBzzMessageCode(0x01)
	register(StoreRequest)

	case object RetrieveRequest extends AbstractBzzMessageCode(0x02)
	register(RetrieveRequest)

	case object Peers extends AbstractBzzMessageCode(0x03)
	register(Peers)

	case object Unknown extends AbstractBzzMessageCode(0xFF)

	val all = this.map.toMap

	def fromByte(b: Byte): BzzMessageCode = this.all.getOrElse(b & 0xFF, Unknown)

	def inRange(code: Byte): Boolean = (Status.command <= code) && (code <= Peers.command)

}