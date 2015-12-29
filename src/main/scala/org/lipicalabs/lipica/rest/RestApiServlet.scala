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
		val response = "NodeId=%s\nBestBlock=[%,d %s]\nKnownPeers=%,d".format(
			SystemProperties.CONFIG.nodeId,
			bestBlock.blockNumber, bestBlock.hash.toShortString,
			worldManager.peersPool.activeCount
		)
		status = 200
		Ok(response)
	}
}
