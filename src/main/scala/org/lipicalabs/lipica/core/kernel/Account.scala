package org.lipicalabs.lipica.core.kernel

import java.security.SecureRandom
import java.util

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.utils.UtilConsts

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 16:00
 * YANAGISAWA, Kentaro
 */
class Account {

	import scala.collection.JavaConversions._

	private var _ecKey: ECKey = null
	private var _address: Address = EmptyAddress

	private val _pendingTransactions = asScalaSet(java.util.Collections.synchronizedSet(new util.HashSet[TransactionLike]))

	def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	def init(aKey: ECKey): Unit = {
		this._ecKey = aKey
		this._address = this._ecKey.getAddress
	}

	def init(): Unit = {
		init(new ECKey(new SecureRandom))
	}

	def address: Address = this._address
	def address_=(v: Address): Unit = this._address = v

	def ecKey: ECKey = this._ecKey

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
