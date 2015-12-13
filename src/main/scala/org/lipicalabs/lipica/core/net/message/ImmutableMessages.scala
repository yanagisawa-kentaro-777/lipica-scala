package org.lipicalabs.lipica.core.net.message

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.p2p._

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
		createHelloMessage(peerId, SystemProperties.CONFIG.listenPort)
	}

	def createHelloMessage(peerId: String, listenPort: Int): HelloMessage = {
		val announcement = buildHelloAnnouncement
		val p2pVersion = P2PHandler.Version
		HelloMessage(p2pVersion, announcement, Capability.all, listenPort, peerId)
	}

	private def buildHelloAnnouncement: String = {
		val version = SystemProperties.CONFIG.projectVersion
		val osName = System.getProperty("os.name").trim
		val system =
			if (osName.contains(" ")) {
				osName.substring(0, osName.indexOf(" "))
			} else if (System.getProperty("java.vm.vendor").toLowerCase.contains("android")) {
				"Android"
			} else {
				osName
			}
		val phrase = SystemProperties.CONFIG.helloPhrase
		String.format("Lipica/v%s/%s/%s/Scala".format(version, phrase, system))
	}

}