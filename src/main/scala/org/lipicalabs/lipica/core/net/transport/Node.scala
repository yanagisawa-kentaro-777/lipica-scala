package org.lipicalabs.lipica.core.net.transport

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/11 19:38
 * YANAGISAWA, Kentaro
 */
class Node {
	//TODO 未実装。

	def id: ImmutableBytes = ???
	def hexId: String = this.id.toHexString
	def hexIdShort: String = this.hexId.substring(0, 8)
}
