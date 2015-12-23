package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.p2p.HelloMessage

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 15:24
 * YANAGISAWA, Kentaro
 */
object HandshakeHelper {

	def getSupportedCapabilities(message: HelloMessage): Seq[Capability] = {
		val configuredCapabilities = Capability.configuredCapabilities
		val (lpcCapabilities, otherCapabilities) = message.capabilities.filter(each => configuredCapabilities.contains(each)).partition(each => each.name == Capability.LPC)
		val lpcCapabilityOfMaxVersion =  lpcCapabilities.reduceOption((accum, each) => if (accum.version < each.version) each else accum)

		(lpcCapabilityOfMaxVersion ++ otherCapabilities).toSeq
	}

}
