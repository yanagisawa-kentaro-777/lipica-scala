package org.lipicalabs.lipica.core.net.p2p

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.message.MessageFactory
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode._
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * 通信経路の維持管理に関連するメッセージを生成するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/05 13:38
 * YANAGISAWA, Kentaro
 */
class P2PMessageFactory extends MessageFactory {
	override def create(code: Byte, encodedBytes: ImmutableBytes) = {
		val result =
			P2PMessageCode.fromByte(code) match {
				case Hello => HelloMessage.decode(encodedBytes)
				case Disconnect => DisconnectMessage.decode(encodedBytes)
				case Ping => P2PMessageFactory.PingMessage
				case Pong => P2PMessageFactory.PongMessage
				case _ => null
			}
		Option(result)
	}
}

/**
 * メッセージのファクトリメソッドを保持するオブジェクトです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/03 21:27
 * YANAGISAWA, Kentaro
 */
object P2PMessageFactory {

	val PingMessage: PingMessage = new PingMessage
	val PongMessage: PongMessage = new PongMessage
	val DisconnectMessage: DisconnectMessage = new DisconnectMessage(ReasonCode.Requested)

	def createHelloMessage(nodeId: NodeId): HelloMessage = {
		createHelloMessage(nodeId, NodeProperties.CONFIG.bindPort)
	}

	def createHelloMessage(nodeId: NodeId, listenPort: Int): HelloMessage = {
		val announcement = buildHelloAnnouncement
		val p2pVersion = P2PHandler.Version
		HelloMessage(p2pVersion, announcement, Capability.all, listenPort, nodeId)
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
		String.format("Lipica/%s/%s/%s/Scala".format(version.toCanonicalString, helloPhrase, system))
	}

}

