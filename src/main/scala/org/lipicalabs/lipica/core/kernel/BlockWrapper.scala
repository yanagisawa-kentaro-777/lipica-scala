package org.lipicalabs.lipica.core.kernel

import java.util.concurrent.atomic.AtomicLong

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * ブロックに幾つかの情報を追加した薄いラッパークラスです。
 *
 * @since 2015/11/21
 * @author YANAGISAWA, Kentaro
 */
class BlockWrapper private(val block: Block, val isNewBlock: Boolean, val nodeId: NodeId) {

	import BlockWrapper._

	/**
	 * ブロックチェーンへの連結に失敗した日時。
	 */
	private val failedTimestamp: AtomicLong = new AtomicLong(0L)
	def importFailedAt: Long = this.failedTimestamp.get
	def importFailedAt_=(v: Long): Unit = this.failedTimestamp.set(v)
	def fireImportFailed(): Unit = {
		val now = System.currentTimeMillis
		this.failedTimestamp.compareAndSet(0L, now)
	}

	/**
	 * 受け付けた日時。
	 */
	private val receivedTimestamp: AtomicLong = new AtomicLong(0L)
	def receivedAt: Long = this.receivedTimestamp.get
	def receivedAt_=(v: Long): Unit = this.receivedTimestamp.set(v)

	def isSolidBlock: Boolean = !isNewBlock || (SolidBlockDurationThresholdMillis < timeSinceReceiving)

	def hash: DigestValue = this.block.hash
	def blockNumber: Long = this.block.blockNumber
	def encode: ImmutableBytes = this.block.encode
	def shortHash: String = this.block.shortHash
	def parentHash: DigestValue = this.block.parentHash

	def timeSinceFailed: Long = {
		val timestamp = this.importFailedAt
		if (timestamp <= 0L) {
			0L
		} else {
			System.currentTimeMillis - timestamp
		}
	}
	def timeSinceReceiving: Long = System.currentTimeMillis - this.receivedAt

	def toBytes: ImmutableBytes = {
		val encodedBlock = this.block.encode
		val encodedImportFailed = RBACCodec.Encoder.encode(BigInt(this.importFailedAt))
		val encodedReceivedAt = RBACCodec.Encoder.encode(BigInt(this.receivedAt))
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

	def apply(block: Block, newBlock: Boolean, nodeId: NodeId): BlockWrapper = new BlockWrapper(block, newBlock, nodeId)
	def apply(block: Block, nodeId: NodeId): BlockWrapper = new BlockWrapper(block, false, nodeId)

	def parse(encodedBytes: ImmutableBytes): BlockWrapper = {
		val decoded = RBACCodec.Decoder.decode(encodedBytes).right.get.items
		val block = Block.decode(decoded.head.items)
		val importFailedAt = decoded(1).bytes.toPositiveLong
		val receivedAt = decoded(2).bytes.toPositiveLong
		val newBlock = decoded(3).bytes.toPositiveLong != 0
		val nodeId = NodeId(decoded(4).bytes)
		val result = apply(block, newBlock, nodeId)
		result.importFailedAt = importFailedAt
		result.receivedAt = receivedAt
		result
	}

}
