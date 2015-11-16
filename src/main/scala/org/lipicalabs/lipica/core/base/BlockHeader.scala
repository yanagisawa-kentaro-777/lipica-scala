package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/16 21:05
 * YANAGISAWA, Kentaro
 */
class BlockHeader {

	/** 親ブロックのSHA3ダイジェスト値。 */
	private var _parentHash: ImmutableBytes = ImmutableBytes.empty
	def parentHash: ImmutableBytes = this._parentHash
	def parentHash_=(v: ImmutableBytes): Unit = this._parentHash = v

	/** uncle list のSHA3ダイジェスト値。 */
	private var _uncleHash: ImmutableBytes = ImmutableBytes.empty
	def uncleHash: ImmutableBytes = this._uncleHash
	def uncleHash_=(v: ImmutableBytes): Unit = this._uncleHash = v

	/**
	 * このブロックのマイニング成功時に、すべての報酬が送られる先の
	 * 160ビットアドレス。
	 */
	private var _coinbase: ImmutableBytes = ImmutableBytes.empty
	def coinbase: ImmutableBytes = this._coinbase
	def coinbase_=(v: ImmutableBytes): Unit = this._coinbase = v

	/**
	 * すべてのトランザクションが実行された後の、
	 * 状態trieのSHA3ルートハッシュ値。
	 */
	private var _stateRoot: ImmutableBytes = ImmutableBytes.empty
	def stateRoot: ImmutableBytes = this._stateRoot
	def stateRoot_=(v: ImmutableBytes): Unit = this._stateRoot = v

	/**
	 * トランザクションを格納したtrieのルートハッシュ値。
	 */
	private var _txTrieRoot: ImmutableBytes = ImmutableBytes.empty
	def txTrieRoot: ImmutableBytes = this._txTrieRoot
	def txTrieRoot_=(v: ImmutableBytes): Unit = this._txTrieRoot = v

	/* The SHA3 256-bit hash of the root node of the trie structure
 * populated with each transaction recipe in the transaction recipes
 * list portion, the trie is populate by [key, val] --> [rlp(index), rlp(tx_recipe)]
 * of the block */
	private var _receiptTrieRoot: ImmutableBytes = ImmutableBytes.empty
	def receiptTrieRoot: ImmutableBytes = this._receiptTrieRoot
	def receiptTrieRoot_=(v: ImmutableBytes): Unit = this._receiptTrieRoot = v

	private var _logsBloom: ImmutableBytes = ImmutableBytes.empty
	def logsBloom: ImmutableBytes = this._logsBloom
	def logsBloom_=(v: ImmutableBytes): Unit = this._logsBloom = v

	/**
	 * このブロックのdifficultyを表現するスカラー値。
	 */
	private var _difficulty: ImmutableBytes = ImmutableBytes.empty
	def difficulty: ImmutableBytes = this._difficulty
	def difficulty_=(v: ImmutableBytes): Unit = this._difficulty = v

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
	 */
	private var _manaLimit: Long = 0L
	def manaLimit: Long = this._manaLimit
	def manaLimit_=(v: Long): Unit = this._manaLimit = v

	/**
	 * このブロックに含まれるトランザクションすべてによって消費されたマナの総量。
	 */
	private var _manaUsed: Long = 0L
	def manaUsed: Long = this._manaUsed
	def manaUsed_=(v: Long): Unit = this._manaUsed = v

	private var _mixHash: ImmutableBytes = ImmutableBytes.empty
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

}
