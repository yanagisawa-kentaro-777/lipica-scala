package org.lipicalabs.lipica.core.net.client

import org.lipicalabs.lipica.core.net.lpc.LpcVersion

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:31
 * YANAGISAWA, Kentaro
 */
case class Capability(name: String, version: Byte) extends Comparable[Capability] {

	override def compareTo(another: Capability): Int = {
		val result = this.name.compareTo(another.name)
		if (result != 0) {
			result
		} else {
			this.version.compareTo(another.version)
		}
	}

	override def toString: String = "%s:%d".format(this.name, this.version)
}

object Capability {
	val P2P = "p2p"
	val LPC = "lpc"
	val SHH = "shh"
	val BZZ = "bzz"

	private val lpcVersions = LpcVersion.SupportedVersions.map(v => Capability(LPC, v.toByte))

	val all = lpcVersions ++ Iterable(Capability(SHH, 0), Capability(BZZ, 0))
}