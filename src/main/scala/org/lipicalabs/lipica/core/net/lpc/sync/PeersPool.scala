package org.lipicalabs.lipica.core.net.lpc.sync

import org.lipicalabs.lipica.core.net.server.Channel
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/12 13:24
 * YANAGISAWA, Kentaro
 */
class PeersPool {
	//TODO

	def peers: Iterable[Channel] = ???

	def getBest: Channel = ???

	def getByNodeId(nodeId: ImmutableBytes): Channel = ???

	def ban(peer: Channel): Unit = ???

	def changeState(stateName: SyncStateName): Unit = ???

	def changeState(stateName: SyncStateName, predicate: (Channel) => Boolean): Unit = ???
}
