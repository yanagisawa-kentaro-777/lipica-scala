package org.lipicalabs.lipica.core.net.lpc.message

import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/08 20:34
 * YANAGISAWA, Kentaro
 */
case class StatusMessage(
	protocolVersion: Byte,
	networkId: Int,
	totalDifficulty: ImmutableBytes,
	bestHash: ImmutableBytes,
	genesisHash: ImmutableBytes) extends LpcMessage {

	override def toEncodedBytes = {
		val encodedProcotolVersion = RBACCodec.Encoder.encode(this.protocolVersion)
		val encodedNetworkId = RBACCodec.Encoder.encode(this.networkId)
		val encodedTotalDifficulty = RBACCodec.Encoder.encode(this.totalDifficulty)
		val encodedBestHash = RBACCodec.Encoder.encode(this.bestHash)
		val encodedGenesisHash = RBACCodec.Encoder.encode(this.genesisHash)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedProcotolVersion, encodedNetworkId, encodedTotalDifficulty, encodedBestHash, encodedGenesisHash))
	}

	override def code = LpcMessageCode.Status.asByte

}

object StatusMessage {
	def decode(encodedBytes: ImmutableBytes): StatusMessage = {
		val items = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val protocolVersion = items.head.asByte
		val networkId = items(1).asInt
		val totalDifficulty = items(2).bytes
		val bestHash = items(3).bytes
		val genesisHash = items(4).bytes
		new StatusMessage(protocolVersion, networkId, totalDifficulty, bestHash, genesisHash)
	}
}