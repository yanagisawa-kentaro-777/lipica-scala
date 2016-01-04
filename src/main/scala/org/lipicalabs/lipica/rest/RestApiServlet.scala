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
		val totalDifficulty = worldManager.blockchain.totalDifficulty

		val response = ("NodeId=%s\n" +
				"ExternalAddress=%s\nBindAddress=%s\n\n" +
				"BestBlock=[%,d %s]\nTotalDifficulty=%,d\n\n" +
				"Active Peers:%,d\n%s\n\n" + "Banned Peers:%,d\n%s\n\n" + "Pending Peers:%,d\n\n").format(
			SystemProperties.CONFIG.nodeId,
			SystemProperties.CONFIG.externalAddress,
			SystemProperties.CONFIG.bindAddress,
			bestBlock.blockNumber, bestBlock.hash.toShortString,
			totalDifficulty,
			worldManager.peersPool.activeCount,
			worldManager.peersPool.peers.map(each => {
				val hostAddress = each.node.address.getAddress.getHostAddress
				val hostName = each.node.address.getAddress.getCanonicalHostName
				if (hostAddress != hostName) {
					"%s\t%s\t%s\t%d\t%s\tTD=%,d".format(each.peerIdShort, hostAddress, hostName, each.node.address.getPort, each.syncStateSummaryAsString, each.totalDifficulty)
				} else {
					"%s\t%s\t%d\t%s\tTD=%,d".format(each.peerIdShort, hostAddress, each.node.address.getPort, each.syncStateSummaryAsString, each.totalDifficulty)
				}
			}).mkString("\n"),
			worldManager.peersPool.bannedPeerIdSet.size,
			worldManager.peersPool.bannedPeerIdSet.map(each => each.substring(0, 8) + "...").mkString("\n"),
			worldManager.peersPool.pendingCount
		)
		status = 200
		Ok(response)
	}
}
