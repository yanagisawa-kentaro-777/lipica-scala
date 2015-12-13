package org.lipicalabs.lipica.core.net.lpc

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:30
 * YANAGISAWA, Kentaro
 */
sealed trait LpcVersion {
	def code: Byte
}

object LpcVersion {
	val SupportedVersions: Seq[Int] = Seq(V0.code)
	def isSupported(code: Byte): Boolean = SupportedVersions.contains(code)

	def fromCode(code: Byte): Option[LpcVersion] = {
		if (code == V0.code) {
			Option(V0)
		} else {
			None
		}
	}
}

case object V0 extends LpcVersion {
	override val code: Byte = 61//TODO
}
