package org.lipicalabs.lipica.core.net.server

import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.net.transport.discover.NodeStatistics
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:55
 * YANAGISAWA, Kentaro
 */
class Channel {
	//TODO 未実装。

	def nodeId: ImmutableBytes = ???
	def peerIdShort: String = ???
	def node: Node = ???
	def nodeStatistics: NodeStatistics = ???

	def isIdle: Boolean = ???
	def isHashRetrieving: Boolean = ???
	def isHashRetrievingDone: Boolean = ???

	def totalDifficulty: BigInt = this.nodeStatistics.lpcTotalDifficulty

}
