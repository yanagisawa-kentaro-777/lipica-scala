package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode
import org.lipicalabs.lipica.core.net.shh.ShhMessageCode
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzMessageCode

import scala.collection.mutable

/**
 * メッセージ種別を表すバイトから、
 * アプリケーションにとってのメッセージ種別を解決するためのクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 13:15
 * YANAGISAWA, Kentaro
 */
class MessageCodesResolver(capabilities: Seq[Capability]) {

	private val offsets = new mutable.HashMap[String, Int]

	def init(caps: Seq[Capability]): Unit = {
		val sorted = caps.sorted
		var offset = P2PMessageCode.User.asByte + 1
		for (capability <- sorted) {
			if (capability.name == Capability.LPC) {
				setLpcOffset(offset)
				offset += LpcMessageCode.all.size
			}
			if (capability.name == Capability.SHH) {
				setShhOffset(offset)
				offset += ShhMessageCode.all.size
			}
			if (capability.name == Capability.BZZ) {
				setBzzOffset(offset)
				offset += BzzMessageCode.all.size + 4
			}
		}
	}

	init(capabilities)

	private def setOffset(cap: String, offset: Int): Unit = this.offsets.put(cap, offset)
	private def getOffset(cap: String): Byte = this.offsets.getOrElse(cap, 0).toByte

	def setLpcOffset(offset: Int): Unit = setOffset(Capability.LPC, offset)
	def setShhOffset(offset: Int): Unit = setOffset(Capability.SHH, offset)
	def setBzzOffset(offset: Int): Unit = setOffset(Capability.BZZ, offset)

	private def resolve(code: Byte, cap: String): Byte = {
		val offset = getOffset(cap)
		(code - offset).toByte
	}
	def resolveP2P(code: Byte): Byte = resolve(code, Capability.P2P)
	def resolveLpc(code: Byte): Byte = resolve(code, Capability.LPC)
	def resolveShh(code: Byte): Byte = resolve(code, Capability.SHH)
	def resolveBzz(code: Byte): Byte = resolve(code, Capability.BZZ)

	private def withOffset(code: Byte, cap: String): Byte = {
		val offset = getOffset(cap)
		(code + offset).toByte
	}
	def withP2POffset(code: Byte): Byte = withOffset(code, Capability.P2P)
	def withLpcOffset(code: Byte): Byte = withOffset(code, Capability.LPC)
	def withShhOffset(code: Byte): Byte = withOffset(code, Capability.SHH)
	def withBzzOffset(code: Byte): Byte = withOffset(code, Capability.BZZ)
}
