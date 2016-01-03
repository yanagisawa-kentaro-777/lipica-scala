package org.lipicalabs.lipica.rest

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.scalatra.ScalatraServlet
import org.scalatra._

/**
 * Created by IntelliJ IDEA.
 * 2015/12/28 13:49
 * YANAGISAWA, Kentaro
 */
class RestApiServlet extends ScalatraServlet {

	get("/:apiVersion/node/status") {
		val worldManager = WorldManager.instance
		val bestBlock = worldManager.blockchain.bestBlock

		val response = "NodeId=%s\nExternalAddress=%s;BindAddress=%s\n\nBestBlock=[%,d %s]\n\nActive Peers:%,d\n%s\n\nBanned Peers:%,d\n%s".format(
			SystemProperties.CONFIG.nodeId,
			SystemProperties.CONFIG.externalAddress,
			SystemProperties.CONFIG.bindAddress,
			bestBlock.blockNumber, bestBlock.hash.toShortString,
			worldManager.peersPool.activeCount,
			worldManager.peersPool.peers.map(each => {
				val hostAddress = each.node.address.getHostAddress
				val hostName = each.node.address.getCanonicalHostName
				if (hostAddress != hostName) {
					"%s\t%s\t%s\t%d".format(each.peerIdShort, hostAddress, hostName, each.node.port)
				} else {
					"%s\t%s\t%d".format(each.peerIdShort, hostAddress, each.node.port)
				}
			}).mkString("\n"),
			worldManager.peersPool.bannedPeerIdSet.size,
			worldManager.peersPool.bannedPeerIdSet.mkString("\n")
		)
		status = 200
		Ok(response)
	}
}
