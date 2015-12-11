package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.net.lpc.V0
import org.lipicalabs.lipica.core.net.lpc.message.GetBlockHashesByNumberMessage
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:06
 * YANAGISAWA, Kentaro
 */
class Lpc0 extends LpcHandler(V0) {
	//TODO 未実装。
	override protected def processBlockHashes(hashes: Seq[ImmutableBytes]) = ???

	override protected def processGetBlockHashesByNumber(message: GetBlockHashesByNumberMessage) = ???

	override def startHashRetrieving() = ???
}
