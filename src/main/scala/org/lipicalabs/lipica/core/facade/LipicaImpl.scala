package org.lipicalabs.lipica.core.facade

import java.math.BigInteger
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

import org.lipicalabs.lipica.core.datastore.RepositoryLike
import org.lipicalabs.lipica.core.kernel._
import org.lipicalabs.lipica.core.facade.listener.{LipicaListenerAdaptor, ManaPriceTracker, LipicaListener}
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.endpoint.PeerClient
import org.lipicalabs.lipica.core.facade.submit.{TransactionExecutor, TransactionTask}
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.program.ProgramResult
import org.slf4j.LoggerFactory


/**
 * 自ノードの実装クラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/25 19:40
 * YANAGISAWA, Kentaro
 */
private[facade] class LipicaImpl extends Lipica {

	import LipicaImpl._


	override val startupTimestamp = System.currentTimeMillis

	override def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private val manaPriceTracker = new ManaPriceTracker

	private val isInitDoneRef = new AtomicBoolean(false)
	private def isInitDone: Boolean = this.isInitDoneRef.get

	private val isShutdownRef = new AtomicBoolean(false)
	private def isShutdown: Boolean = this.isShutdownRef.get

	override def startup(): Unit = {
		this.synchronized {
			if (isInitDone) {
				return
			}
			this.isInitDoneRef.set(true)
			//このタイミングで、各コンポーネントが起動される。
			val motherboard = componentsMotherboard
			//マナ価格ウォッチャーを登録する。
			motherboard.listener.addListener(this.manaPriceTracker)
		}
	}

	override def blockchain: Blockchain = componentsMotherboard.blockchain

	override def repository: RepositoryLike = componentsMotherboard.repository

	override def addListener(listener: LipicaListener) = componentsMotherboard.addListener(listener)

	override def client: PeerClient = this.componentsMotherboard.client

	override def submitTransaction(tx: TransactionLike): Future[TransactionLike] = {
		val task = new TransactionTask(tx, this.componentsMotherboard)
		TransactionExecutor.submitTransaction(task)
	}

	override def pendingTransactions: Set[TransactionLike] = this.componentsMotherboard.blockchain.pendingTransactions

	/**
	 * 最近のトランザクションにおけるマナ価格の実績に基いて、
	 * おおむね妥当だと思われるマナ価格を計算して返します。
	 *
	 * 25%程度のトランザクションが、
	 * この価格かそれ以下で実行されている実績値です。
	 * より確実に優先的に実行してもらいたい場合には、20%程度割増すると良いでしょう。
	 */
	override def recentManaPrice = this.manaPriceTracker.getManaPrice

	override def exitOn(number: Long) = this.componentsMotherboard.blockchain.exitOn = number

	override def shutdown(): Unit = {
		this.synchronized {
			if (isShutdown) {
				return
			}
			this.isShutdownRef.set(true)
			logger.info("<Lipica> Shutting down.")
			ComponentsMotherboard.instance.shutdown()
			logger.info("<Lipica> Shut down complete.")
		}
	}

	/**
	 * Factory for general transaction
	 *
	 * @param nonce - アカウントによって実行されたトランザクション数。
	 * @param manaPrice - 手数料の相場。
	 * @param mana - このトランザクションに必要なマナの両。
	 * @param receiveAddress - このトランザクションの宛先。
	 * @param value - 額。
	 * @param data - コントラクトの初期化コード、もしくはメッセージの付随データ。
	 * @return 作成されたトランザクション。
	 */
	override def createTransaction(nonce: BigInteger, manaPrice: BigInteger, mana: BigInteger, receiveAddress: Array[Byte], value: BigInteger, data: Array[Byte]): TransactionLike = {
		val nonceBytes = BigIntBytes(nonce)
		val manaPriceBytes = BigIntBytes(manaPrice)
		val manaBytes = BigIntBytes(mana)
		val valueBytes = BigIntBytes(ImmutableBytes.asUnsignedByteArray(value))

		Transaction(nonceBytes, manaPriceBytes, manaBytes, Address160(receiveAddress), valueBytes, ImmutableBytes(data))
	}

	override def callConstantFunction(receiveAddress: String, function: CallTransaction.Function, funcArgs: Any*): Option[ProgramResult] = {
		val tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L, receiveAddress, 0, function, funcArgs)
		tx.sign(ImmutableBytes.create(32))

		val bestBlock = componentsMotherboard.blockchain.bestBlock
		val programContextFactory = componentsMotherboard.programContextFactory
		val executor = new org.lipicalabs.lipica.core.kernel.TransactionExecutor(
			tx, bestBlock.coinbase, componentsMotherboard.repository, componentsMotherboard.blockStore, programContextFactory, bestBlock, new LipicaListenerAdaptor, 0
		)
		executor.localCall = true

		executor.prepare()
		executor.execute()
		executor.go()
		executor.finalization()

		executor.resultOption
	}

}

object LipicaImpl {
	private val logger = LoggerFactory.getLogger("general")
}