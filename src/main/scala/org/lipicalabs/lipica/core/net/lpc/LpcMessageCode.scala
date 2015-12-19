package org.lipicalabs.lipica.core.net.lpc

import org.lipicalabs.lipica.core.net.message.Command

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/12/08 20:02
 * YANAGISAWA, Kentaro
 */

sealed trait LpcMessageCode extends Command {
	def command: Int
	def asByte: Byte = this.command.toByte
}


object LpcMessageCode {

	abstract class AbstractLpcMessageCode(override val command: Int) extends LpcMessageCode

	private val map = new mutable.HashMap[Int, LpcMessageCode]
	private def register(code: LpcMessageCode): Unit = this.map.put(code.command, code)

	/**
	 * 現在の状態について通知する。
	 */
	case object Status extends AbstractLpcMessageCode(0x00)
	register(Status)

	case object NewBlockHashes extends AbstractLpcMessageCode(0x01)
	register(NewBlockHashes)

	case object Transactions extends AbstractLpcMessageCode(0x02)
	register(Transactions)

	case object GetBlockHashes extends AbstractLpcMessageCode(0x03)
	register(GetBlockHashes)

	case object BlockHashes extends AbstractLpcMessageCode(0x04)
	register(BlockHashes)

	case object GetBlocks extends AbstractLpcMessageCode(0x05)
	register(GetBlocks)

	case object Blocks extends AbstractLpcMessageCode(0x06)
	register(Blocks)

	case object NewBlock extends AbstractLpcMessageCode(0x07)
	register(NewBlock)

	case object GetBlockHashesByNumber extends AbstractLpcMessageCode(0x08)
	register(GetBlockHashesByNumber)

	case object Unknown extends AbstractLpcMessageCode(0xFF)

	val all = this.map.toMap

	def fromByte(b: Byte): LpcMessageCode = this.all.getOrElse(b & 0xFF, Unknown)

	def inRange(code: Byte): Boolean = (Status.command <= code) && (code <= GetBlockHashesByNumber.command)

}
