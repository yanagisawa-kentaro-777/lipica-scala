package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.{RBACCodec, UtilConsts, ImmutableBytes}

/**
 * あるアカウントの残高等の情報を表現するクラス。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:03
 * YANAGISAWA, Kentaro
 */
class AccountState(_n: BigInt, _b: BigInt) {

	def this() = this(UtilConsts.Zero, UtilConsts.Zero)

	/**
	 * アカウントがこれまでに送信したトランザクション
	 * （メッセージコール／コントラクト作成）の数。
	 */
	private var _nonce: BigInt = _n
	def nonce: BigInt = this._nonce
	def nonce_=(v: BigInt): Unit = this._nonce = v
	def incrementNonce(): Unit = {
		this.nonce = this.nonce + 1
		this.isDirty = true
	}

	/**
	 * アカウントの残高。
	 */
	private var _balance: BigInt = _b
	def balance: BigInt = this._balance
	def balance_=(v: BigInt): Unit = {
		this._balance = v
		this.isDirty = true
	}
	def addToBalance(value: BigInt): BigInt = {
		this.balance = this.balance + value
		this.isDirty = true
		this.balance
	}
	def subtractFromBalance(value: BigInt): BigInt = {
		this.balance = this.balance - value
		this.isDirty = true
		this.balance
	}

	/**
	 * ストレージの状態を表すルートハッシュ値。
	 */
	private var _storageRoot: ImmutableBytes = DigestUtils.EmptyTrieHash
	def storageRoot: ImmutableBytes = this._storageRoot
	def storageRoot_=(v: ImmutableBytes): Unit = {
		this._storageRoot = v
		this.isDirty = true
	}

	/**
	 * このアカウントに結び付けられたプログラムコードのハッシュ値。
	 */
	private var _codeHash: ImmutableBytes = DigestUtils.EmptyDataHash
	def codeHash: ImmutableBytes = this._codeHash
	def codeHash_=(v: ImmutableBytes): Unit = this._codeHash = v

	private var _isDirty: Boolean = false
	def isDirty: Boolean = this._isDirty
	def isDirty_=(v: Boolean): Unit = this._isDirty = v

	private var _isDeleted: Boolean = false
	def isDeleted: Boolean = this._isDeleted
	def isDeleted_=(v: Boolean): Unit = this._isDeleted = v

	def encode: ImmutableBytes = {
		val nonce = RBACCodec.Encoder.encode(this.nonce)
		val balance = RBACCodec.Encoder.encode(this.balance)
		val storageRoot = RBACCodec.Encoder.encode(this.storageRoot)
		val codeHash = RBACCodec.Encoder.encode(this.codeHash)
		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(nonce, balance, storageRoot, codeHash))
	}

	def createClone: AccountState = {
		val result = new AccountState
		result.balance = this.balance
		result.nonce = this.nonce
		result.codeHash = this.codeHash
		result.storageRoot = this.storageRoot
		this.isDirty = false
		result
	}

	override def toString: String = "AccountState[Nonce=%,d; Balance=%,d; StorageRoot=%s, CodeHash=%s]".format(this.nonce, this.balance, this.storageRoot, this.codeHash)

}

object AccountState {

	def decode(bytes: ImmutableBytes): AccountState = {
		val items = RBACCodec.Decoder.decode(bytes).right.get.items
		val result = new AccountState
		result.nonce = items.head.asPositiveBigInt
		result.balance = items(1).asPositiveBigInt
		result.storageRoot = items(2).bytes
		result.codeHash = items(3).bytes
		result
	}
}