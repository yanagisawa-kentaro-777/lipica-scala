package org.lipicalabs.lipica.core.utils

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:47
 * YANAGISAWA, Kentaro
 */
object JsonUtils {

	private def isNullOrEmpty(s: String): Boolean = (s eq null) || s.isEmpty

	def parseHexStringToImmutableBytes(s: String): ImmutableBytes = {
		if (isNullOrEmpty(s)) {
			ImmutableBytes.empty
		} else {
			var v = s.trim
			v =
				if (v.toLowerCase.startsWith("0x")) {
					v.substring(2)
				} else {
					v
				}
			ImmutableBytes.parseHexString(v)
		}
	}

	def parseHexStringToLong(s: String): Long = parseHexStringToImmutableBytes(s).toPositiveLong

}
