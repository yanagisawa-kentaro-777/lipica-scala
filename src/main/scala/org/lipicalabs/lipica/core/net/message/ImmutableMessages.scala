package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.p2p._
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:27
 * YANAGISAWA, Kentaro
 */
object ImmutableMessages {

	val PingMessage: PingMessage = new PingMessage
	val PongMessage: PongMessage = new PongMessage
	val GetPeersMessage: GetPeersMessage = new GetPeersMessage
	val DisconnectMessage: DisconnectMessage = new DisconnectMessage(ReasonCode.Requested)

	def createHelloMessage(peerId: String): HelloMessage = {
		createHelloMessage(peerId, NodeProperties.CONFIG.bindPort)
	}

	def createHelloMessage(peerId: String, listenPort: Int): HelloMessage = {
		val announcement = buildHelloAnnouncement
		val p2pVersion = P2PHandler.Version
		HelloMessage(p2pVersion, announcement, Capability.all, listenPort, ImmutableBytes.parseHexString(peerId))
	}

	private def buildHelloAnnouncement: String = {
		val version = NodeProperties.CONFIG.moduleVersion
		val osName = System.getProperty("os.name").trim
		val system =
			if (osName.contains(" ")) {
				osName.substring(0, osName.indexOf(" "))
			} else if (System.getProperty("java.vm.vendor").toLowerCase.contains("android")) {
				"Android"
			} else {
				osName
			}
		val helloPhrase = NodeProperties.CONFIG.helloPhrase
		String.format("Lipica/%s/%s/%s/Scala".format(version, helloPhrase, system))
	}

}
