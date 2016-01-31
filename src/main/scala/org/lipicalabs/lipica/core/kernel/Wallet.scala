package org.lipicalabs.lipica.core.kernel

import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicInteger}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.crypto.elliptic_curve.{ECKeyLike, ECKeyPair}
import org.lipicalabs.lipica.core.crypto.scrypt.SCrypt
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}
import org.slf4j.LoggerFactory

/**
 * 特定少数の鍵ペア（＝アドレス）の取引および残高を管理するクラスです。
 *
 * Created by IntelliJ IDEA.
 * @since 2015/11/23 13:36
 * @author YANAGISAWA, Kentaro
 */
class Wallet {

	import Wallet._
	import scala.collection.JavaConversions._

	/**
	 * このインスタンスで保持しているトランザクションの集合を表す連想配列です。（txハッシュ値 -> tx）
	 */
	private val walletTransactions = mapAsScalaConcurrentMap(new ConcurrentHashMap[DigestValue, WalletTransaction])

	/**
	 * このインスタンスでの管理対象とするアカウントの集合です。
	 */
	private val accountsMap = mapAsScalaConcurrentMap(new ConcurrentHashMap[Address, Account])

	private val listenersRef = new AtomicReference[Seq[WalletListener]](Seq.empty)
	private def listeners: Seq[WalletListener] = this.listenersRef.get

	/**
	 * 新たなアカウントを生成して、そのアドレスを返します。
	 */
	def addNewAccount(): Address = {
		val account = new Account(ECKeyPair(new SecureRandom))
		val address = account.address
		this.accountsMap.put(address, account)
		notifyListeners()
		address
	}

	/**
	 * 渡された秘密鍵からアドレスを生成して、それをこのインスタンスに記録します。
	 */
	def importPrivateKey(privateKey: ImmutableBytes): Address = {
		val account = new Account(ECKeyPair.fromPrivateKey(privateKey.toPositiveBigInt))
		val address = account.address
		this.accountsMap.put(address, account)
		notifyListeners()
		address
	}

	/**
	 * このインスタンスで管理しているアドレスの残高総額を返します。
	 */
	def totalBalance: BigInt = this.accountsMap.values.foldLeft(UtilConsts.Zero)((accum, account) => accum + account.balance)

	/**
	 * トランザクションをこのインスタンスに記録します。
	 */
	def addTransaction(tx: TransactionLike): WalletTransaction = {
		val result =
			this.walletTransactions.get(tx.hash) match {
				case Some(walletTx) =>
					walletTx.incrementApproved
					walletTx
				case None =>
					val walletTx = new WalletTransaction(tx)
					this.walletTransactions.put(tx.hash, walletTx)
					walletTx
			}
		this.applyTransaction(tx)
		result
	}

	def addTransactions(txs: Iterable[TransactionLike]): Unit = txs.foreach(this.addTransaction)

	def removeTransaction(tx: TransactionLike): Unit = {
		this.walletTransactions.remove(tx.hash)
	}

	def removeTransactions(txs: Iterable[TransactionLike]): Unit = txs.foreach(this.removeTransaction)

	private def applyTransaction(tx: TransactionLike): Unit = {
		val senderAddress = tx.senderAddress
		this.accountsMap.get(senderAddress).foreach {
			sender => {
				sender.addPendingTransaction(tx)
				logger.info("<Wallet> Pending tx added to account [%s], tx [%s]".format(sender.address, tx.hash))
			}
		}
		val receiverAddress = tx.receiverAddress
		this.accountsMap.get(receiverAddress).foreach {
			receiver => {
				receiver.addPendingTransaction(tx)
				logger.info("<Wallet> Pending tx added to account [%s], tx [%s]".format(receiver.address, tx.hash))
			}
		}
		notifyListeners()
	}

	def processBlock(block: Block): Unit = {
		accounts.foreach(_.clearPendingTransactions())
		notifyListeners()
	}

	def accounts: Iterable[Account] = this.accountsMap.values

	def addListener(listener: WalletListener): Unit = {
		val currentListeners = this.listeners
		this.listenersRef.set(listener +: currentListeners)
	}
	private def notifyListeners(): Unit = {
		this.listeners.foreach(_.valueChanged())
	}

}

object Wallet {
	private val logger = LoggerFactory.getLogger("wallet")

	/**
	 * gethの方式に従ったwalletファイルを復号します。
	 */
	def decrypt(cipherText: ImmutableBytes, password: String, iv: ImmutableBytes, dklen: Int, N: Int, r: Int, p: Int, salt: ImmutableBytes): ImmutableBytes = {
		val key = ImmutableBytes(SCrypt.scrypt(
			password.getBytes(StandardCharsets.UTF_8),
			salt.toByteArray,
			N, r, p, dklen
		)).copyOfRange(0, 16).toByteArray
		val cipher = Cipher.getInstance("AES/CTR/NoPadding")
		val keySpec = new SecretKeySpec(key, "AES")
		val ivSpec = new IvParameterSpec(iv.toByteArray)
		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
		val result = cipher.doFinal(cipherText.toByteArray)
		ImmutableBytes(result)
	}
}

class WalletTransaction(private val tx: TransactionLike) {
	private val approvedRef = new AtomicInteger(0)

	def incrementApproved: Int = this.approvedRef.incrementAndGet
	def approved: Int = this.approvedRef.get
}

trait WalletListener {
	def valueChanged(): Unit
}

/**
 * Walletで利用されるアカウント情報を表すクラスです。
 *
 * Created by IntelliJ IDEA.
 * @since 2015/11/21 16:00
 * @author YANAGISAWA, Kentaro
 */
class Account(private val ecKey: ECKeyLike) {

	import scala.collection.JavaConversions._

	val address: Address = this.ecKey.toAddress

	private val _pendingTransactions = asScalaSet(java.util.Collections.synchronizedSet(new util.HashSet[TransactionLike]))

	def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance


	def pendingTransactions: Set[TransactionLike] = this._pendingTransactions.toSet

	def nonce: BigInt = this.componentsMotherboard.repository.getNonce(this.address)

	def balance: BigInt = {
		var result = this.componentsMotherboard.repository.getBalance(this.address).getOrElse(UtilConsts.Zero)
		this._pendingTransactions.synchronized {
			if (this._pendingTransactions.nonEmpty) {
				for (tx <- this._pendingTransactions) {
					if (tx.senderAddress == this.address) {
						result -= tx.value.positiveBigInt
					}
					if (tx.receiverAddress == this.address) {
						result += tx.value.positiveBigInt
					}
				}
				//TODO feeの計算。
			}
		}
		result
	}

	def addPendingTransaction(tx: TransactionLike): Unit = {
		this._pendingTransactions.synchronized {
			this._pendingTransactions.add(tx)
		}
	}

	def clearPendingTransactions(): Unit = {
		this._pendingTransactions.synchronized {
			this._pendingTransactions.clear()
		}
	}

}
