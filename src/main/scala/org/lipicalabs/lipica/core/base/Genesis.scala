package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:56
 * YANAGISAWA, Kentaro
 */
class Genesis private[base](_header: BlockHeader, val premine: Map[ImmutableBytes, AccountState]) extends PlainBlock(_header, Seq.empty[TransactionLike], Seq.empty[BlockHeader]) {
	//
}
