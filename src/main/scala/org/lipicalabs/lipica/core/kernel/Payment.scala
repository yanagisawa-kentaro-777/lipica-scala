package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.db.RepositoryLike
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * 通貨の移動ないし採掘の処理を一元的に実行するためのオブジェクトです。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/22 12:28
 * YANAGISAWA, Kentaro
 */
object Payment {

	private val logger = LoggerFactory.getLogger("execute")

	/**
	 * 支払い元から支払い先に資金を移動します。
	 * @return 支払先の実行後残高。
	 */
	def transfer(repository: RepositoryLike, fromAddress: ImmutableBytes, toAddress: ImmutableBytes, value: BigInt, reason: Reason): BigInt = {
		repository.addBalance(fromAddress, -value)
		val result = repository.addBalance(toAddress, value)
		if (logger.isDebugEnabled) {
			logger.debug("<Payment> Transferred %,d from %s to %s (%s).".format(value, fromAddress.toShortString, toAddress.toShortString, reason))
		}
		result
	}

	/**
	 * 支払い元なしに、通貨を生成して採掘者に支払います。
	 */
	def reward(repository: RepositoryLike, coinbase: ImmutableBytes, value: BigInt, reason: Reason): Unit = {
		repository.addBalance(coinbase, value)
		if (logger.isDebugEnabled) {
			logger.debug("<Payment> Rewarded %,d to %s (%s).".format(value, coinbase.toShortString, reason))
		}
	}

	/**
	 * トランザクション手数料の支払いに関連する資金移動を実行します。
	 */
	def txFee(repository: RepositoryLike, address: ImmutableBytes, value: BigInt, reason: Reason): Unit = {
		repository.addBalance(address, value)
		if (logger.isDebugEnabled) {
			logger.debug("<Payment> Tx fee %,d to %s (%s).".format(value, address.toShortString, reason))
		}
	}

	/**
	 * 採掘もしくは移動の原因となる処理を表す trait です。
	 */
	sealed trait Reason

	/** Genesisブロックに含まれる premine。 */
	case object PremineReward extends Reason

	/** 先に引き当てられるトランザクション報酬。 */
	case object TxFeeAdvanceWithdrawal extends Reason

	/** トランザクション報酬の余剰分払い戻し。 */
	case object TxFeeRefund extends Reason

	/** minerに支払われるトランザクション報酬。 */
	case object TxFee extends Reason

	/** minerに支払われるuncle報酬。 */
	case object UncleReward extends Reason

	/** minerに支払われるブロック報酬。 */
	case object BlockReward extends Reason

	/** アカウントどうしの授受。 */
	case object TxSettlement extends Reason

	/** コントラクト作成に伴う授受。 */
	case object ContractCreationTx extends Reason

	/** コントラクト実行に伴う授受。 */
	case object ContractInvocationTx extends Reason

	/** コントラクトの破棄（Suidide）に伴う遺贈。 */
	case object Bequest extends Reason

}

