package org.lipicalabs.lipica.core.net.client

import java.nio.charset.StandardCharsets

import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/30 20:31
 * YANAGISAWA, Kentaro
 */
case class Capability(name: String, version: Byte) extends Comparable[Capability] {

	def toEncodedBytes: ImmutableBytes = {
		val encodedName = RBACCodec.Encoder.encode(this.name)
		val encodedVersion = RBACCodec.Encoder.encode(this.version)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedName, encodedVersion))
	}

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
	val LPC = "eth"//TODO
	val SHH = "shh"
	val BZZ = "bzz"

	private val lpcVersions = LpcVersion.SupportedVersions.map(v => Capability(LPC, v.toByte))

	val all = lpcVersions ++ Iterable(Capability(SHH, 0), Capability(BZZ, 0))

	def decode(encodedBytes: ImmutableBytes): Capability = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		decode(items)
	}

	def decode(items: Seq[DecodedResult]): Capability = {
		Capability(items.head.bytes.asString(StandardCharsets.UTF_8), items(1).bytes.head)
	}
}