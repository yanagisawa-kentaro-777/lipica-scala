package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.crypto.digest.{Digest256, EmptyDigest, DigestValue, DigestUtils}
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes}
import org.lipicalabs.lipica.core.validator.block_header_rules.ProofOfWorkRule
import org.lipicalabs.lipica.core.validator.parent_rules.DifficultyRule

/**
 * ブロックの主要な情報を保持するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/16 21:05
 * YANAGISAWA, Kentaro
 */
class BlockHeader {

	/**
	 * 親ブロックの256ビットダイジェスト値。
	 */
	private var _parentHash: DigestValue = EmptyDigest
	def parentHash: DigestValue = this._parentHash
	def parentHash_=(v: DigestValue): Unit = this._parentHash = v

	/**
	 * uncle list の256ビットダイジェスト値。
	 * this.encodeUncles.digest256 と等しいはずである。
	 */
	private var _unclesHash: DigestValue = DigestUtils.EmptySeqHash
	def unclesHash: DigestValue = this._unclesHash
	def unclesHash_=(v: DigestValue): Unit = this._unclesHash = v

	/**
	 * このブロックのマイニング成功時に、すべての報酬が送られる先の
	 * 160ビットアドレス。
	 */
	private var _coinbase: Address = EmptyAddress
	def coinbase: Address = this._coinbase
	def coinbase_=(v: Address): Unit = this._coinbase = v

	/**
	 * すべてのトランザクションが実行された後の、
	 * 状態trieのルートハッシュ値。
	 */
	private var _stateRoot: DigestValue = DigestUtils.EmptyTrieHash
	def stateRoot: DigestValue = this._stateRoot
	def stateRoot_=(v: DigestValue): Unit = this._stateRoot = v

	/**
	 * トランザクションを格納したtrieのルートハッシュ値。
	 */
	private var _txTrieRoot: DigestValue = DigestUtils.EmptyTrieHash
	def txTrieRoot: DigestValue = this._txTrieRoot
	def txTrieRoot_=(v: DigestValue): Unit = this._txTrieRoot = v

	/**
	 * Transaction Receipt を格納した trie のルートハッシュ値。
	 * おそらく、Block.calculateTxTrieRoot と同様の方法によって算出可能。
	 */
	private var _receiptTrieRoot: DigestValue = DigestUtils.EmptyTrieHash
	def receiptTrieRoot: DigestValue = this._receiptTrieRoot
	def receiptTrieRoot_=(v: DigestValue): Unit = this._receiptTrieRoot = v

	private var _logsBloomFilter: BloomFilter = BloomFilter.empty
	def logsBloomFilter: BloomFilter = this._logsBloomFilter
	def logsBloomFilter_=(v: BloomFilter): Unit = this._logsBloomFilter = v

	/**
	 * このブロックのdifficultyを表現するスカラー値。
	 * calculateDifficulty によって、前のブロックおよびタイムスタンプから計算される。
	 */
	private var _difficulty: BigIntBytes = BigIntBytes.zero
	def difficulty: BigIntBytes = this._difficulty
	def difficulty_=(v: BigIntBytes): Unit = this._difficulty = v

	private var _timestamp: Long = 0L
	def timestamp: Long = this._timestamp
	def timestamp_=(v: Long): Unit = this._timestamp = v

	/**
	 * このブロックが持っている祖先の数。
	 * genesisブロックは、ゼロである。
	 */
	private var _blockNumber: Long = 0L
	def blockNumber: Long = this._blockNumber
	def blockNumber_=(v: Long): Unit = this._blockNumber = v

	/**
	 * １ブロックあたりのマナ消費上限を表すスカラー値。
	 *
	 * この制限は、トランザクションの実行者が指定する mana limit と混同されてはならない。
	 * この値は bitcoin のブロックサイズのようなもので、
	 * ブロックの巨大化にともなう合意効率の低下を避けるためのパラメータである。
	 * minerはこの値を、一定の条件下で変動させることができる。
	 * 40 & 41 の式を参照。
	 */
	private var _manaLimit: BigIntBytes = BigIntBytes.zero
	def manaLimit: BigIntBytes = this._manaLimit
	def manaLimit_=(v: BigIntBytes): Unit = this._manaLimit = v

	/**
	 * このブロックに含まれるトランザクションすべてによって消費されたマナの総量。
	 */
	private var _manaUsed: Long = 0L
	def manaUsed: Long = this._manaUsed
	def manaUsed_=(v: Long): Unit = this._manaUsed = v

	/**
	 * ethash において意味を持つ mix-hash。
	 */
	private var _mixHash: DigestValue = DigestUtils.EmptyDataHash
	def mixHash: DigestValue = this._mixHash
	def mixHash_=(v: DigestValue): Unit = this._mixHash = v

	private var _extraData: ImmutableBytes = ImmutableBytes.empty
	def extraData: ImmutableBytes = this._extraData
	def extraData_=(v: ImmutableBytes): Unit = this._extraData = v

	/**
	 * PoWの計算に利用する256ビット値。
	 */
	private var _nonce: ImmutableBytes = ImmutableBytes.empty
	def nonce: ImmutableBytes = this._nonce
	def nonce_=(v: ImmutableBytes): Unit = this._nonce = v

	def isGenesis: Boolean = this.blockNumber == 0

	def encode: ImmutableBytes = this.encode(withNonce = true)

	def encode(withNonce: Boolean): ImmutableBytes = {
		val encodedParentHash = RBACCodec.Encoder.encode(this.parentHash)
		val encodedUnclesHash = RBACCodec.Encoder.encode(this.unclesHash)
		val encodedCoinbase = RBACCodec.Encoder.encode(this.coinbase)
		val encodedStateRoot = RBACCodec.Encoder.encode(this.stateRoot)
		val encodedTxTrieRoot = RBACCodec.Encoder.encode(Option(this.txTrieRoot).getOrElse(DigestUtils.EmptyTrieHash))
		val encodedReceiptTrieRoot = RBACCodec.Encoder.encode(Option(this.receiptTrieRoot).getOrElse(DigestUtils.EmptyTrieHash))
		val encodedLogsBloom = RBACCodec.Encoder.encode(this.logsBloomFilter.bits)
		val encodedDifficulty = RBACCodec.Encoder.encode(this.difficulty)
		val encodedBlockNumber = RBACCodec.Encoder.encode(BigInt(this.blockNumber))
		val encodedManaLimit = RBACCodec.Encoder.encode(this.manaLimit)
		val encodedManaUsed = RBACCodec.Encoder.encode(BigInt(this.manaUsed))
		val encodedTimestamp = RBACCodec.Encoder.encode(BigInt(this.timestamp))
		val encodedExtraData = RBACCodec.Encoder.encode(this.extraData)

		if (withNonce) {
			val encodedMixHash = RBACCodec.Encoder.encode(this.mixHash)
			val encodedNonce = RBACCodec.Encoder.encode(this.nonce)
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedParentHash, encodedUnclesHash, encodedCoinbase, encodedStateRoot, encodedTxTrieRoot, encodedReceiptTrieRoot, encodedLogsBloom, encodedDifficulty, encodedBlockNumber, encodedManaLimit, encodedManaUsed, encodedTimestamp, encodedExtraData, encodedMixHash, encodedNonce))
		} else {
			RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedParentHash, encodedUnclesHash, encodedCoinbase, encodedStateRoot, encodedTxTrieRoot, encodedReceiptTrieRoot, encodedLogsBloom, encodedDifficulty, encodedBlockNumber, encodedManaLimit, encodedManaUsed, encodedTimestamp, encodedExtraData))
		}
	}

	/**
	 * このブロックの difficulty に対応する、許容されるダイジェスト値の上限値を返します。
	 */
	def getProofOfWorkBoundary: BigInt = ProofOfWorkRule.calculateProofOfWorkBoundary(this.difficulty)

	/**
	 * Powの中心アルゴリズム！
	 * この値が PoW Boundary を下回っている必要があります。
	 */
	def calculateProofOfWorkValue: BigIntBytes = {
		//リトルエンディアンに変換する。
		val revertedNonce = this.nonce.reverse
		val hashWithoutNonce = this.encode(withNonce = false).digest256
		val seed = hashWithoutNonce.bytes ++ revertedNonce
		val seedHash = seed.digest512
		val concat = seedHash.bytes ++ this.mixHash.bytes

		BigIntBytes(concat.digest256.bytes)
	}

	/**
	 * difficulty 遷移の中心アルゴリズム！
	 */
	def calculateDifficulty(parent: BlockHeader): BigInt = {
		DifficultyRule.calculateDifficulty(parent = parent, newBlockNumber = this.blockNumber, newTimeStamp = this.timestamp)
	}

	def toStringWithSuffix(suffix: String): String = {
		val builder = (new StringBuilder).
			append("parentHash=").append(this.parentHash.toHexString).append(suffix).
			append("unclesHash=").append(this.unclesHash.toHexString).append(suffix).
			append("coinbase=").append(this.coinbase.toHexString).append(suffix).
			append("stateRoot=").append(this.stateRoot.toHexString).append(suffix).
			append("txTrieHash=").append(this.txTrieRoot.toHexString).append(suffix).
			append("receiptsTrieHash=").append(this.receiptTrieRoot.toHexString).append(suffix).
			append("difficulty=").append(this.difficulty).append(suffix).
			append("blockNumber=").append(this.blockNumber).append(suffix).
			append("manaLimit=").append(this.manaLimit).append(suffix).
			append("manaUsed=").append(this.manaUsed).append(suffix).
			append("timestamp=").append(this.timestamp).append(suffix).
			append("extraData=").append(this.extraData.toHexString).append(suffix).
			append("mixHash=").append(this.mixHash.toHexString).append(suffix).
			append("nonce=").append(this.nonce.toHexString).append(suffix)
		builder.toString()
	}

	override def toString: String = toStringWithSuffix("\n")

	def toFlatString: String = toStringWithSuffix("")

}

object BlockHeader {
	def decode(decodedResult: DecodedResult): BlockHeader = {
		val result = new BlockHeader
		result.parentHash = Digest256(decodedResult.items.head.bytes)
		result.unclesHash = Digest256(decodedResult.items(1).bytes)
		result.coinbase = Address160(decodedResult.items(2).bytes)
		result.stateRoot = Digest256(decodedResult.items(3).bytes)
		result.txTrieRoot = Digest256(decodedResult.items(4).bytes)
		result.receiptTrieRoot = Digest256(decodedResult.items(5).bytes)
		result.logsBloomFilter = BloomFilter(decodedResult.items(6).bytes)
		result.difficulty = BigIntBytes(decodedResult.items(7).bytes)

		result.blockNumber = decodedResult.items(8).bytes.toPositiveBigInt.longValue()
		result.manaLimit = BigIntBytes(decodedResult.items(9).bytes)
		result.manaUsed = decodedResult.items(10).bytes.toPositiveBigInt.longValue()
		result.timestamp = decodedResult.items(11).bytes.toPositiveBigInt.longValue()

		result.extraData = decodedResult.items(12).bytes
		result.mixHash = DigestValue(decodedResult.items(13).bytes)
		result.nonce = decodedResult.items(14).bytes

		result
	}

	def encodeUncles(uncles: Seq[BlockHeader]): ImmutableBytes = {
		val seqOfEncodedBytes = uncles.map(_.encode)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seqOfEncodedBytes)
	}

}