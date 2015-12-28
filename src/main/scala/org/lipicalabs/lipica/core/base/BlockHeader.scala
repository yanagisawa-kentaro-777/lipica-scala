package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}
import org.lipicalabs.lipica.core.validator.{ProofOfWorkRule, DifficultyRule}

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
	private var _parentHash: ImmutableBytes = ImmutableBytes.empty
	def parentHash: ImmutableBytes = this._parentHash
	def parentHash_=(v: ImmutableBytes): Unit = this._parentHash = v

	/**
	 * uncle list の256ビットダイジェスト値。
	 * this.encodeUncles.digest256 と等しいはずである。
	 */
	private var _unclesHash: ImmutableBytes = DigestUtils.EmptySeqHash
	def unclesHash: ImmutableBytes = this._unclesHash
	def unclesHash_=(v: ImmutableBytes): Unit = this._unclesHash = v

	/**
	 * このブロックのマイニング成功時に、すべての報酬が送られる先の
	 * 160ビットアドレス。
	 */
	private var _coinbase: ImmutableBytes = ImmutableBytes.empty
	def coinbase: ImmutableBytes = this._coinbase
	def coinbase_=(v: ImmutableBytes): Unit = this._coinbase = v

	/**
	 * すべてのトランザクションが実行された後の、
	 * 状態trieのルートハッシュ値。
	 */
	private var _stateRoot: ImmutableBytes = DigestUtils.EmptyTrieHash
	def stateRoot: ImmutableBytes = this._stateRoot
	def stateRoot_=(v: ImmutableBytes): Unit = this._stateRoot = v

	/**
	 * トランザクションを格納したtrieのルートハッシュ値。
	 */
	private var _txTrieRoot: ImmutableBytes = DigestUtils.EmptyTrieHash
	def txTrieRoot: ImmutableBytes = this._txTrieRoot
	def txTrieRoot_=(v: ImmutableBytes): Unit = this._txTrieRoot = v

	/**
	 * Transaction Receipt を格納した trie のルートハッシュ値。
	 * おそらく、Block.calculateTxTrieRoot と同様の方法によって算出可能。
	 */
	private var _receiptTrieRoot: ImmutableBytes = DigestUtils.EmptyTrieHash
	def receiptTrieRoot: ImmutableBytes = this._receiptTrieRoot
	def receiptTrieRoot_=(v: ImmutableBytes): Unit = this._receiptTrieRoot = v

	private var _logsBloom: ImmutableBytes = ImmutableBytes.empty
	def logsBloom: ImmutableBytes = this._logsBloom
	def logsBloom_=(v: ImmutableBytes): Unit = this._logsBloom = v

	/**
	 * このブロックのdifficultyを表現するスカラー値。
	 * calculateDifficulty によって、前のブロックおよびタイムスタンプから計算される。
	 */
	private var _difficulty: ImmutableBytes = ImmutableBytes.empty
	def difficulty: ImmutableBytes = this._difficulty
	def difficulty_=(v: ImmutableBytes): Unit = this._difficulty = v
	def difficultyAsBigInt: BigInt = this.difficulty.toPositiveBigInt

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
	private var _manaLimit: ImmutableBytes = ImmutableBytes.empty
	def manaLimit: ImmutableBytes = this._manaLimit
	def manaLimit_=(v: ImmutableBytes): Unit = this._manaLimit = v

	/**
	 * このブロックに含まれるトランザクションすべてによって消費されたマナの総量。
	 */
	private var _manaUsed: Long = 0L
	def manaUsed: Long = this._manaUsed
	def manaUsed_=(v: Long): Unit = this._manaUsed = v

	/**
	 * ethash において意味を持つ mix-hash。
	 */
	private var _mixHash: ImmutableBytes = DigestUtils.EmptyDataHash
	def mixHash: ImmutableBytes = this._mixHash
	def mixHash_=(v: ImmutableBytes): Unit = this._mixHash = v

	private var _extraData: ImmutableBytes = ImmutableBytes.empty
	def extraData: ImmutableBytes = this._extraData
	def extraData_=(v: ImmutableBytes): Unit = this._extraData = v

	/**
	 * 充分な計算が実行されたことを証明する、256ビットハッシュ値。
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
		val encodedLogsBloom = RBACCodec.Encoder.encode(this.logsBloom)
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
	def getProofOfWorkBoundary: ImmutableBytes = ProofOfWorkRule.getProofOfWorkBoundary(this.difficulty)

	/**
	 * Powの中心アルゴリズム！
	 */
	def calculateProofOfWorkValue: ImmutableBytes = {
		//リトルエンディアンに変換する。
		val revertedNonce = this.nonce.reverse
		val hashWithoutNonce = this.encode(withNonce = false).digest256
		val seed = hashWithoutNonce ++ revertedNonce
		val seedHash = seed.digest512
		val concat = seedHash ++ this.mixHash

		concat.digest256
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
			append("difficulty=").append(this.difficulty.toHexString).append(suffix).
			append("blockNumber=").append(this.blockNumber.toHexString).append(suffix).
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
		result.parentHash = decodedResult.items.head.bytes
		result.unclesHash = decodedResult.items(1).bytes
		result.coinbase = decodedResult.items(2).bytes
		result.stateRoot = decodedResult.items(3).bytes
		result.txTrieRoot = decodedResult.items(4).bytes
		result.receiptTrieRoot = decodedResult.items(5).bytes
		result.logsBloom = decodedResult.items(6).bytes
		result.difficulty = decodedResult.items(7).bytes

		result.blockNumber = decodedResult.items(8).bytes.toPositiveBigInt.longValue()
		result.manaLimit = decodedResult.items(9).bytes
		result.manaUsed = decodedResult.items(10).bytes.toPositiveBigInt.longValue()
		result.timestamp = decodedResult.items(11).bytes.toPositiveBigInt.longValue()

		result.extraData = decodedResult.items(12).bytes
		result.mixHash = decodedResult.items(13).bytes
		result.nonce = decodedResult.items(14).bytes

		result
	}

	def encodeUncles(uncles: Seq[BlockHeader]): ImmutableBytes = {
		val seqOfEncodedBytes = uncles.map(_.encode)
		RBACCodec.Encoder.encodeSeqOfByteArrays(seqOfEncodedBytes)
	}

}