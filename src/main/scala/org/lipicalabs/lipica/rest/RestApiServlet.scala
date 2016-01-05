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
		val blockchain = worldManager.blockchain
		val bestBlock = blockchain.bestBlock
		val totalDifficulty = blockchain.totalDifficulty

		val peersPool = worldManager.peersPool
		val nodeManager = worldManager.nodeManager
		val syncManager = worldManager.syncManager

		val bannedPeers = peersPool.bannedPeersMap

		val response = ("NodeId=%s\n" +
				"ExternalAddress=%s\nBindAddress=%s\n\n" +
				"BestBlock=[%,d %s]\nTotalDifficulty=%,d\n\n" +
				"ProcessingBlock=%s\n\n" +
				"LowerTD=%,d\nHighestKnownTD=%,d\n\n" +
				"Active Peers:%,d\n%s\n\n" + "Banned Peers:%,d\n%s\n\n" + "Pending Peers:%,d\n\n" +
				"NumNodeHandlers:%,d\nNumNodesInTable:%,d\n\n").format(
			SystemProperties.CONFIG.nodeId,
			SystemProperties.CONFIG.externalAddress,
			SystemProperties.CONFIG.bindAddress,

			bestBlock.blockNumber, bestBlock.hash.toShortString,
			totalDifficulty,
			blockchain.processingBlockOption.map(_.summaryString(short = true)).getOrElse("None"),

			syncManager.lowerUsefulDifficulty,
			syncManager.highestKnownDifficulty,

			peersPool.activeCount,
			peersPool.peers.map(each => {
				val hostAddress = each.node.address.getAddress.getHostAddress
				val hostName = each.node.address.getAddress.getCanonicalHostName
				if (hostAddress != hostName) {
					"%s\t%s\t%s\t%d\t%s\tTD=%,d".format(each.peerIdShort, hostAddress, hostName, each.node.address.getPort, each.syncStateSummaryAsString, each.totalDifficulty)
				} else {
					"%s\t%s\t%d\t%s\tTD=%,d".format(each.peerIdShort, hostAddress, each.node.address.getPort, each.syncStateSummaryAsString, each.totalDifficulty)
				}
			}).mkString("\n"),
			bannedPeers.size,
			bannedPeers.map(entry => entry._1.toShortString + " -> " + entry._2).mkString("\n"),
			peersPool.pendingCount,

			nodeManager.numberOfKnownNodes,
			nodeManager.table.getNodeCount
		)
		status = 200
		Ok(response)
	}
}
