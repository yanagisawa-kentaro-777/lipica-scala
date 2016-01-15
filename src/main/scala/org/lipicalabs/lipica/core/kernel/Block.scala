package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * ブロックは大略、
 * ヘッダ、トランザクションリスト、アンクルリストの３要素から構成される。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:04
 * YANAGISAWA, Kentaro
 */
trait Block {

	/**
	 * このブロックのコアとなる情報の集合。
	 */
	def blockHeader: BlockHeader

	/**
	 * このブロックの親のダイジェスト値。
	 */
	def parentHash: DigestValue

	/**
	 * アンクルリスト部分のダイジェスト値。
	 */
	def unclesHash: DigestValue

	/**
	 * このブロックの採掘に成功した場合の報酬が送られるアドレス。
	 */
	def coinbase: Address

	/**
	 * このブロックに含まれるすべてのトランザクションの
	 * 実行および終了処理が完了した時点における、状態のルートダイジェスト値。
	 */
	def stateRoot: DigestValue
	def stateRoot_=(v: DigestValue): Unit

	/**
	 * トランザクションリスト部のダイジェスト値。
	 */
	def txTrieRoot: DigestValue

	/**
	 * トランザクションレシートリスト部のダイジェスト値。
	 */
	def receiptsRoot: DigestValue

	/**
	 * トランザクションリスト内のログ情報を格納したブルームフィルタ。
	 */
	def logsBloom: ImmutableBytes

	/**
	 * このブロックにおける難度。
	 * 前ブロックの難度および経過時間から計算可能。
	 */
	def difficulty: ImmutableBytes
	def difficultyAsBigInt: BigInt

	/**
	 * このブロックに至るまでの難度の合計。
	 */
	def cumulativeDifficulty: BigInt

	/**
	 * このブロックの祖先の数と等しい連番。
	 * Genesisブロックにおいてはゼロとなる。
	 */
	def blockNumber: Long

	/**
	 * １ブロックあたりの消費マナ上限。
	 */
	def manaLimit: ImmutableBytes

	/**
	 * このブロック内において消費されたマナ。
	 */
	def manaUsed: Long

	/**
	 * このブロック誕生時におけるUNIX時刻。
	 */
	def timestamp: Long

	/**
	 * このブロックに関係がある32バイト以下のデータ。
	 */
	def extraData: ImmutableBytes

	/**
	 * Proof of Work となるダイジェスト値。
	 */
	def mixHash: DigestValue

	/**
	 * 64ビット値。Proof of Work の構成要素。
	 */
	def nonce: ImmutableBytes
	def nonce_=(v: ImmutableBytes): Unit

	def hash: DigestValue

	def transactions: Seq[TransactionLike]

	def uncles: Seq[BlockHeader]

	def encode: ImmutableBytes

	def encodeWithoutNonce: ImmutableBytes

	def isParentOf(another: Block): Boolean

	def isGenesis: Boolean

	def isEqualTo(another: Block): Boolean

	def shortHash: String

	def summaryString(short: Boolean): String

	override def equals(o: Any): Boolean = {
		try {
			this.hash == o.asInstanceOf[Block].hash
		} catch {
			case e: Throwable => false
		}
	}

}

class PlainBlock private[kernel](override val blockHeader: BlockHeader, override val transactions: Seq[TransactionLike], override val uncles: Seq[BlockHeader]) extends Block {

	override def hash = this.blockHeader.encode.digest256

	override def parentHash = this.blockHeader.parentHash

	override def unclesHash = this.blockHeader.unclesHash

	override def coinbase = this.blockHeader.coinbase

	override def stateRoot = this.blockHeader.stateRoot

	override def stateRoot_=(v: DigestValue): Unit = {
		this.blockHeader.stateRoot = v
	}

	override def txTrieRoot = this.blockHeader.txTrieRoot

	override def receiptsRoot = this.blockHeader.receiptTrieRoot

	override def logsBloom = this.blockHeader.logsBloom

	override def difficulty = this.blockHeader.difficulty

	override def difficultyAsBigInt = this.blockHeader.difficultyAsBigInt

	override def cumulativeDifficulty = {
		val thisDifficulty = this.blockHeader.difficultyAsBigInt
		this.uncles.foldLeft(thisDifficulty)((accum, each) => accum + each.difficultyAsBigInt)
	}

	override def timestamp = this.blockHeader.timestamp

	override def blockNumber = this.blockHeader.blockNumber

	override def manaLimit = this.blockHeader.manaLimit

	override def manaUsed = this.blockHeader.manaUsed

	override def extraData = this.blockHeader.extraData

	override def mixHash = this.blockHeader.mixHash

	override def nonce = this.blockHeader.nonce

	override def nonce_=(v: ImmutableBytes): Unit = {
		this.blockHeader.nonce = v
	}

	override def isParentOf(another: Block): Boolean = this.hash == another.parentHash

	override def isGenesis: Boolean = this.blockHeader.isGenesis

	override def isEqualTo(another: Block): Boolean = this.hash == another.hash

	override def encode: ImmutableBytes = {
		val encodedHeader = this.blockHeader.encode
		val encodedTransactions = RBACCodec.Encoder.encodeSeqOfByteArrays(this.transactions.map(_.toEncodedBytes))
		val encodedUncles = RBACCodec.Encoder.encodeSeqOfByteArrays(this.uncles.map(_.encode))
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedHeader, encodedTransactions, encodedUncles))
	}

	override def encodeWithoutNonce: ImmutableBytes = this.blockHeader.encode(withNonce = false)

	override def toString: String = {
		val result = new StringBuilder
		result.append("Encoded=[").append(this.encode.toHexString).append("]\n")
		result.append("BlockData[ ")
		result.append("hash=").append(this.hash.toHexString).append("\n")
		result.append(this.blockHeader.toString)

		result.append("\nUncles [\n")
		for (uncle <- this.uncles) {
			result.append(uncle.toString).append("\n")
		}
		result.append("]")

		result.append("\nTransactions [\n")
		for (tx <- this.transactions) {
			result.append(tx.toString).append("\n")
		}
		result.append("]")
		result.append("\n]")

		result.toString()
	}

	override def summaryString(short: Boolean): String = {
		val template = "Block[BlockNumber=%,d; Hash=%s; ParentHash=%s; Coinbase=%s; Difficulty=%,d]"
		template.format(this.blockNumber, this.hash.toShortString, this.parentHash.toShortString, this.coinbase.toShortString, this.difficultyAsBigInt)
	}

	def toFlatString: String = {
		val result = new StringBuilder
		result.append("BlockData[ ")
		result.append("hash=").append(this.hash.toHexString).append("\n")
		result.append(this.blockHeader.toFlatString)

		for (tx <- this.transactions) {
			result.append("\n").append(tx.toString)
		}
		result.append("]")
		result.toString()
	}

	override def shortHash: String = this.hash.toShortString

}

object Block {
	private[kernel] val logger = LoggerFactory.getLogger("general")

	val BlockReward =
		if (NodeProperties.CONFIG.isFrontier) {
			BigInt("5000000000000000000")
		} else {
			BigInt("1500000000000000000")
		}

	val UncleReward = BlockReward * BigInt(15) / BigInt(16)

	val InclusionReward = BlockReward / BigInt(32)

	def decode(encodedBytes: ImmutableBytes): Block = {
		val decodedResult = RBACCodec.Decoder.decode(encodedBytes).right.get
		decode(decodedResult.items)
	}

	def decode(items: Seq[RBACCodec.Decoder.DecodedResult]): Block = {
		val blockHeader = BlockHeader.decode(items.head)
		val transactions = items(1).items.map(each => Transaction.decode(each))
		val uncles = items(2).items.map(BlockHeader.decode)
		new PlainBlock(blockHeader, transactions, uncles)
	}

}