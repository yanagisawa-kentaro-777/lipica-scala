package org.lipicalabs.lipica.core.base

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.scrypt.SCrypt
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}
import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer
import scala.collection.{mutable, JavaConversions}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/23 13:36
 * YANAGISAWA, Kentaro
 */
class Wallet {

	import Wallet._

	private val walletTransactions = JavaConversions.mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, WalletTransaction])

	private val rows = new mutable.HashMap[ImmutableBytes, Account]

	private var high: Long = 0

	private def worldManager: WorldManager = WorldManager.instance

	private val listeners = new ArrayBuffer[WalletListener]

	def addNewAccount(): Unit = {
		val account = new Account
		account.init()
		val address = ImmutableBytes(account.ecKey.getAddress)
		this.rows.put(address, account)
		notifyListeners()
	}

	def importKey(privateKey: ImmutableBytes): Unit = {
		val account = new Account
		account.init(ECKey.fromPrivate(privateKey.toPositiveBigInt.bigInteger))
		val address = ImmutableBytes(account.ecKey.getAddress)
		this.rows.put(address, account)
		notifyListeners()
	}

	def totalBalance: BigInt = this.rows.values.foldLeft(UtilConsts.Zero)((accum, account) => accum + account.balance)

	def addByWalletTransaction(transaction: TransactionLike): WalletTransaction = {
		val result = new WalletTransaction(transaction)
		this.walletTransactions.put(transaction.hash, result)
		result
	}

	/**
	 * トランザクションを承認します。
	 */
	def addTransaction(tx: TransactionLike): WalletTransaction = {
		val result =
			this.walletTransactions.get(tx.hash) match {
				case Some(walletTx) =>
					walletTx.inrementApproved()
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

	def applyTransaction(tx: TransactionLike): Unit = {
		val senderAddress = tx.senderAddress
		this.rows.get(senderAddress).foreach {
			sender => {
				sender.addPendingTransaction(tx)
				logger.info("<Wallet> Pending tx added to account [%s], tx [%s]".format(sender.address, tx.hash))
			}
		}
		val receiverAddress = tx.receiverAddress
		this.rows.get(receiverAddress).foreach {
			receiver => {
				receiver.addPendingTransaction(tx)
				logger.info("<Wallet> Pending tx added to account [%s], tx [%s]".format(receiver.address, tx.hash))
			}
		}
		notifyListeners()
	}

	/**
	 * gethの方式に従ったwalletファイルを複合
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

	def load(path: Path): Unit = {
		//TODO 未実装。
		/*
		         <wallet high="8933">
             <row id=1>
                 <address nonce="1" >7c63d6d8b6a4c1ec67766ae123637ca93c199935<address/>
                 <privkey>roman<privkey/>
                 <value>20000000<value/>
             </row>
             <row id=2>
                 <address nonce="6" >b5da3e0ba57da04f94793d1c334e476e7ce7b873<address/>
                 <privkey>cow<privkey/>
                 <value>900099909<value/>
             </row>
         </wallet>
		 */
	}

	def save(path: Path): Unit = {
		//TODO 未実装。
		/*
		         <wallet high="8933">
             <row id=1>
                 <address nonce="1" >7c63d6d8b6a4c1ec67766ae123637ca93c199935<address/>
                 <privkey>roman<privkey/>
                 <value>20000000<value/>
             </row>
             <row id=2>
                 <address nonce="6" >b5da3e0ba57da04f94793d1c334e476e7ce7b873<address/>
                 <privkey>cow<privkey/>
                 <value>900099909<value/>
             </row>
         </wallet>
		 */
	}

	def processBlock(block: Block): Unit = {
		accounts.foreach(_.clearPendingTransactions())
		notifyListeners()
	}

	def accounts: Iterable[Account] = this.rows.values

	def addListener(listener: WalletListener): Unit = {
		this.listeners.append(listener)
	}
	private def notifyListeners(): Unit = {
		this.listeners.foreach(_.valueChanged())
	}

}

object Wallet {
	private val logger = LoggerFactory.getLogger("wallet")
}

class WalletTransaction(private val tx: TransactionLike) {
	private var _approved: Int = 0

	def inrementApproved(): Unit = this._approved += 1
	def approved: Int = this._approved
}

trait WalletListener {
	def valueChanged(): Unit
}