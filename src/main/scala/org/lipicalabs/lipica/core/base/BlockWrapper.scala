package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/11/21
 * @author YANAGISAWA, Kentaro
 */
class BlockWrapper private(val block: Block, val isNewBlock: Boolean, val nodeId: ImmutableBytes) {

	import BlockWrapper._

	private var _importFailedAt: Long = 0L
	def importFailedAt: Long = this._importFailedAt
	def importFailedAt_=(v: Long): Unit = this._importFailedAt = v
	def fireImportFailed(): Unit = {
		if (this._importFailedAt == 0L) {
			this._importFailedAt = System.currentTimeMillis()
		}
	}

	private var _receivedAt: Long = 0L
	def receivedAt: Long = this._receivedAt
	def receivedAt_=(v: Long): Unit = this._receivedAt = v

	def isSolidBlock: Boolean = !isNewBlock || (SolidBlockDurationThresholdMillis < timeSinceReceiving)

	def hash: ImmutableBytes = this.block.hash
	def blockNumber: Long = this.block.blockNumber
	def encode: ImmutableBytes = this.block.encode
	def shortHash: String = this.block.shortHash
	def parentHash: ImmutableBytes = this.block.parentHash

	def timeSinceFailed: Long = {
		if (this._importFailedAt == 0) {
			0
		} else {
			System.currentTimeMillis - _importFailedAt
		}
	}
	def timeSinceReceiving: Long = System.currentTimeMillis - this._receivedAt

	def toBytes: ImmutableBytes = {
		val encodedBlock = this.block.encode
		val encodedImportFailed = RBACCodec.Encoder.encode(BigInt(this._importFailedAt))
		val encodedReceivedAt = RBACCodec.Encoder.encode(BigInt(this._receivedAt))
		val encodedNewBlock = RBACCodec.Encoder.encode(if (this.isNewBlock) 1.toByte else 0.toByte)
		val encodedNodeId = RBACCodec.Encoder.encode(this.nodeId)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedBlock, encodedImportFailed, encodedReceivedAt, encodedNewBlock, encodedNodeId))
	}

	override def equals(o: Any): Boolean = {
		try {
			this.block.isEqualTo(o.asInstanceOf[BlockWrapper].block)
		} catch {
			case e: Throwable => false
		}
	}

}

object BlockWrapper {
	private val SolidBlockDurationThresholdMillis: Long = 60L * 1000L

	def apply(block: Block, newBlock: Boolean, nodeId: ImmutableBytes): BlockWrapper = new BlockWrapper(block, newBlock, nodeId)
	def apply(block: Block, nodeId: ImmutableBytes): BlockWrapper = new BlockWrapper(block, false, nodeId)

	def parse(encodedBytes: ImmutableBytes): BlockWrapper = {
		val decoded = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val block = Block.decode(decoded.head.items)
		val importFailedAt = decoded(1).bytes.toPositiveLong
		val receivedAt = decoded(2).bytes.toPositiveLong
		val newBlock = decoded(3).bytes.toPositiveLong != 0
		val nodeId = decoded(4).bytes
		val result = apply(block, newBlock, nodeId)
		result.importFailedAt = importFailedAt
		result.receivedAt = receivedAt
		result
	}

}
