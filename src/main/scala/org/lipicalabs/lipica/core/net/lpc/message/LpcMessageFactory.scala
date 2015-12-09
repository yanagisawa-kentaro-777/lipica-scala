package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.MessageFactory
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/09 21:10
 * YANAGISAWA, Kentaro
 */
class LpcMessageFactory extends MessageFactory {

	override def create(code: Byte, encodedBytes: ImmutableBytes) = {
		val result =
			LpcMessageCode.fromByte(code) match {
				case LpcMessageCode.Status => StatusMessage.decode(encodedBytes)
				case LpcMessageCode.NewBlockHashes => NewBlockHashesMessage.decode(encodedBytes)
				case LpcMessageCode.Transactions => TransactionsMessage.decode(encodedBytes)
				case LpcMessageCode.GetBlockHashes => GetBlockHashesMessage.decode(encodedBytes)
				case LpcMessageCode.BlockHashes => BlockHashesMessage.decode(encodedBytes)
				case LpcMessageCode.GetBlocks => GetBlocksMessage.decode(encodedBytes)
				case LpcMessageCode.Blocks => BlocksMessage.decode(encodedBytes)
				case LpcMessageCode.NewBlock => NewBlockMessage.decode(encodedBytes)
				case LpcMessageCode.GetBlockHashesByNumber => GetBlockHashesByNumberMessage.decode(encodedBytes)
				case _ => null
			}
		Option(result)
	}

}
