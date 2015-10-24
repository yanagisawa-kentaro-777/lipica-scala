package org.lipicalabs.lipica.core

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.utils.RBACCodec

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
object Driver {

	def main(args: Array[String]): Unit = {
		val s = "F86E12F86B80881BC16D674EC8000094CD2A3D9F938E13CD947EC05ABC7FE734DF8DD8268609184E72A00064801BA0C52C114D4F5A3BA904A9B3036E5E118FE0DBB987FE3955DA20F2CD8F6C21AB9CA06BA4C2874299A55AD947DBC98A25EE895AABF6B625C26C435E84BFD70EDF2F69";
		val data = Hex.decodeHex(s.toCharArray)

		val decoded = RBACCodec.Decoder.decode(data).right.get

		decoded.printRecursively(System.out)

	}

}
