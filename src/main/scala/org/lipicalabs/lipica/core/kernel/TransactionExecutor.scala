package org.lipicalabs.lipica.core.kernel

import java.util.concurrent.atomic.AtomicLong

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.datastore.{RepositoryLike, RepositoryTrackLike, BlockStore}
import org.lipicalabs.lipica.core.facade.listener.LipicaListener
import org.lipicalabs.lipica.core.utils.{ByteUtils, ErrorLogger, UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.PrecompiledContracts.PrecompiledContract
import org.lipicalabs.lipica.core.vm._
import org.lipicalabs.lipica.core.vm.program.Program.ProgramException
import org.lipicalabs.lipica.core.vm.program.{ProgramResult, Program}
import org.lipicalabs.lipica.core.vm.program.context.ProgramContextFactory
import org.slf4j.LoggerFactory

/**
 * 自ノードにおいてトランザクションを実行する実行者クラスです。
 *
 * 送信者アドレスから受信者アドレスへの送金処理と、
 * VMの構築およびVM上でのコントラクトコードの実行を主宰します。
 *
 * １個のトランザクションを実行するために、
 * このクラスのインスタンス１個が生成され、
 * このインスタンスの内部において多くの可変の状態を保持します。
 * そのため、このクラスを利用する際には、
 * 複数のメソッドを意図された順序で、同一のスレッドからコールする必要があります。
 *
 * Created by IntelliJ IDEA.
 * @since 2015/11/22 13:32
 * @author YANAGISAWA, Kentaro
 */
class TransactionExecutor(
	private val tx: TransactionLike,
	private val coinbase: Address,
	private val track: RepositoryLike,
	private val blockStore: BlockStore,
	private val programContextFactory: ProgramContextFactory,
	private val currentBlock: Block,
	private val listener: LipicaListener,
	private val manaUsedInTheBlock: Long
) {
	import TransactionExecutor._

	private val cacheTrack: RepositoryTrackLike = this.track.startTracking
	private var vm: VM = null
	private var program: Program = null
	private var precompiledContract: Option[PrecompiledContract] = None
	private var result: ProgramResult = new ProgramResult
	private var _logs: Seq[LogInfo] = Seq.empty

	private var readyToExecute: Boolean = false
	private var basicTxCost: Long = 0L
	private var endMana: Long = 0L

	private var _localCall: Boolean = false
	def localCall: Boolean = this._localCall
	def localCall_=(v: Boolean): Unit = this._localCall = v

	private val startNanosRef = new AtomicLong(0L)

	/**
	 * トランザクションの実行を準備します。
	 *
	 * マナや残高の引当検査を行い、問題がない場合にのみ「実行可能フラグ」を立てる、という処理です。
	 */
	def prepare(): Unit = {
		//実行開始日時。
		this.startNanosRef.set(System.nanoTime)

		if (this.localCall) {
			this.readyToExecute = true
			return
		}
		val txManaLimit = this.tx.manaLimit.positiveBigInt.longValue()
		//新たなトランザクションを処理すると、このブロックのマナ上限を超えるか？
		val exceedingLimit = this.currentBlock.manaLimit.positiveBigInt < BigInt(this.manaUsedInTheBlock + txManaLimit)
		if (exceedingLimit) {
			logger.info("<TxExecutor> Mana limit reached: Limit of block: %,d, Already used: %,d, Required: %,d".format(
				this.currentBlock.manaLimit.positiveBigInt, this.manaUsedInTheBlock, txManaLimit
			))
			return
		}
		//トランザクションの基本コストを、指定された上限でまかなえるか。
		this.basicTxCost = transactionCost(this.tx)
		if (txManaLimit < this.basicTxCost) {
			logger.info("<TxExecutor> Not enough mana for tx execution: %s < %s".format(txManaLimit, this.basicTxCost))
			return
		}
		//送信者アカウントの累積トランザクション実行数と、このトランザクションの番号とは合っているか。
		val reqNonce = this.track.getNonce(this.tx.senderAddress)
		val txNonce = this.tx.nonce.positiveBigInt
		if (reqNonce != txNonce) {
			logger.info("<TxExecutor> Invalid nonce: Required: %s != Tx: %s".format(reqNonce, txNonce))
			return
		}
		//送金額と、消費マナ上限分の金額とをまかなえるだけの送信者残高はあるか。
		val txManaCost = this.tx.manaPrice.positiveBigInt * BigInt(txManaLimit)
		val totalCost = this.tx.value.positiveBigInt + txManaCost
		val senderBalance = this.track.getBalance(this.tx.senderAddress).getOrElse(UtilConsts.Zero)
		if (senderBalance < totalCost) {
			logger.info("<TxExecutor> Not enough coin: Required: %s, Sender balance: %s".format(totalCost, senderBalance))
			return
		}
		this.readyToExecute = true
	}

	/**
	 * トランザクション実行の第一段階を処理します。
	 */
	def execute(): Unit = {
		if (!this.readyToExecute) {
			return
		}
		if (!localCall) {
			//送信者の累積トランザクション実行数を増加させる。
			this.track.incrementNonce(this.tx.senderAddress)
			//トランザクション実行コストを、送信者の残高から引き当てる。
			val txManaPrice = this.tx.manaPrice.positiveBigInt
			val txManaLimit = this.tx.manaLimit.positiveBigInt
			val txManaCost = txManaPrice * txManaLimit
			Payment.txFee(this.track, tx.senderAddress, -txManaCost, Payment.TxFeeAdvanceWithdrawal)
			logger.info("<TxExecutor> Withdraw in advance: TxManaCost: %,d, ManaPrice: %,d, ManaLimit: %,d".format(txManaCost, txManaPrice, txManaLimit))
		}
		if (this.tx.isContractCreation) {
			//コントラクトを作成する。
			create()
		} else {
			//トランザクションを実行する。
			//これは単純送金かもしれないし、コントラクト呼び出しかもしれない。
			call()
		}
	}

	/**
	 * コントラクト作成処理を実行します。
	 */
	private def create(): Unit = {
		val newContractAddress = this.tx.contractAddress.get
		if (this.tx.data.isEmpty) {
			//コントラクト作成なのに入力データがない、というのは、
			//コントラクトの内容を定義できないので無意味だが、
			//いちおう基本料金だけ受け取って、アカウントを作っておく。
			this.endMana = this.tx.manaLimit.positiveBigInt.longValue() - this.basicTxCost
			this.cacheTrack.createAccount(newContractAddress)
		} else {
			//「コントラクト作成コード」を実行する。
			//実行環境を定義して、VMを構築し、実行対象のプログラムを定義する。
			val context = this.programContextFactory.createProgramContext(tx, currentBlock, cacheTrack, blockStore)
			this.vm = new VM
			//実行対象のプログラムは、入力データそのもの。
			this.program = new Program(this.tx.data, context, this.tx)

			//同一アドレスが既存だった場合のため、ストレージ項目をコピーしておく。
			this.program.storage.getContractDetails(newContractAddress).foreach {
				contractDetails => {
					for (key <- contractDetails.storageKeys) {
						this.program.storageSave(key, VMWord.Zero)
					}
				}
			}
		}
		//送信者から、新たなコントラクトアカウントへの送金を実行する。
		val endowment = this.tx.value.positiveBigInt
		Payment.transfer(this.cacheTrack, this.tx.senderAddress, newContractAddress, endowment, Payment.ContractCreationTx)
	}

	/**
	 * トランザクション実行の第一段階を行います。
	 */
	private def call(): Unit = {
		if (!readyToExecute) {
			return
		}
		val targetAddress = this.tx.receiverAddress
		//システム定義済みのコントラクト呼び出しか？
		this.precompiledContract = PrecompiledContracts.getContractForAddress(VMWord(targetAddress.bytes))
		this.precompiledContract match {
			case Some(contract) =>
				//定義済みコントラクトの呼び出しだった。
				if (logger.isDebugEnabled) {
					logger.debug("<TxExecutor> Precompiled contract invocation: [%s]".format(targetAddress))
				}
				val requiredMana = contract.manaForData(this.tx.data)
				val txManaLimit = this.tx.manaLimit.positiveBigInt.longValue()
				if (!localCall && txManaLimit < requiredMana) {
					//コストオーバー。
					return
				} else {
					this.endMana = txManaLimit - requiredMana - this.basicTxCost
					contract.execute(this.tx.data)
				}
			case None =>
				//システム定義済みコントラクトの呼び出しではない。
				if (logger.isTraceEnabled) {
					logger.trace("<TxExecutor> User defined contract invocation? [%s]".format(targetAddress))
				}
				//ユーザー定義されたコントラクトの呼び出しか？
				this.track.getCode(targetAddress) match {
					case Some(code) =>
						//ユーザー定義されたコントラクトの呼び出しだった。
						if (logger.isDebugEnabled) {
							logger.debug("<TxExecutor> Contract loaded: [%s]=[%,d bytes]".format(targetAddress, code.length))
						}
						val context = this.programContextFactory.createProgramContext(this.tx, this.currentBlock, this.cacheTrack, this.blockStore)
						this.vm = new VM
						this.program = new Program(code, context, this.tx)
					case None =>
						//ユーザー定義されたコントラクトの呼び出しではなかった。
						//つまり単純送金である。
						if (logger.isTraceEnabled) {
							logger.trace("<TxExecutor> Normal tx.")
						}
						this.endMana = this.tx.manaLimit.positiveBigInt.longValue() - this.basicTxCost
				}
		}
		//相手が通常アカウントであれコントラクトアカウントであれ、送金を実行する。
		val endowment = this.tx.value.positiveBigInt
		Payment.transfer(this.cacheTrack, this.tx.senderAddress, targetAddress, endowment, Payment.TxSettlement)
	}

	/**
	 * トランザクション実行の第２段階を実行します。
	 */
	def go(): Unit = {
		if (!readyToExecute) {
			return
		}
		if (this.vm eq null) {
			return
		}
		try {
			//ここに来たということは、単純な送金トランザクションではなく、
			//VMで実行すべきコードがあるということ。
			//構築済みのVMでコードを再生する。
			this.program.spendMana(transactionCost(this.tx), "Tx Cost")
			this.vm.play(this.program)
			this.result = this.program.result
			this.endMana = (this.tx.manaLimit.positiveBigInt - BigInt(this.result.manaUsed)).longValue()

			if (this.tx.isContractCreation) {
				val returnDataManaValue = result.hReturn.length * ManaCost.CreateData
				if (returnDataManaValue <= this.endMana) {
					if (logger.isDebugEnabled) {
						logger.debug("<TxExecutor> Contract creation: [%s]=[%,d Bytes]".format(this.tx.contractAddress, result.hReturn.length))
					}
					this.endMana -= returnDataManaValue
					this.cacheTrack.saveCode(this.tx.contractAddress.get, result.hReturn)
				} else {
					//足りない。
					if (logger.isDebugEnabled) {
						logger.debug("<TxExecutor> Contract creation: Mana NOT enough: %,d < %,d".format(this.endMana, returnDataManaValue))
					}
					result.hReturn = ImmutableBytes.empty
				}
			}
			if (this.result.exception ne null) {
				this.result.clearDeletedAccounts()
				result.clearLogs()
				result.resetFutureRefund()
				throw result.exception
			}
		} catch {
			case e: Throwable =>
				if (e.isInstanceOf[ProgramException]) {
					//マナ不足等でもここに来る。ゆえに、ここに来ることは想定外とは言えない。
					logger.info("<TxExecutor> Exception caught %s: %s".format(e.getClass.getSimpleName, e.getMessage))
				} else {
					ErrorLogger.logger.warn("<TxExecutor> Exception caught: %s".format(e.getClass.getSimpleName), e)
					logger.warn("<TxExecutor> Exception caught: %s".format(e.getClass.getSimpleName), e)
				}
				this.cacheTrack.rollback()
				this.endMana = 0
		}
	}

	/**
	 * 後始末処理を実行します。
	 */
	def finalization(): Unit = {
		if (!this.readyToExecute) {
			return
		}
		this.cacheTrack.commit()
		//トランザクション実行結果のサマリーを構築する。
		val summaryBuilder = TransactionExecutionSummary.builder(this.tx).manaLeftOver(this.endMana)
		if (this.result ne null) {
			this.result.addFutureRefund(this.result.deletedAccounts.size * ManaCost.SuicideRefund)
			val manaRefund = this.result.futureRefund min (this.result.manaUsed / 2)
			//val address = if (this.tx.isContractCreation) this.tx.contractAddress.get else this.tx.receiverAddress
			this.endMana += manaRefund

			summaryBuilder.manaUsed(BigInt(this.result.manaUsed)).manaRefund(BigInt(manaRefund)).
				deletedAccounts(this.result.deletedAccounts).
				internalTransactions(this.result.internalTransactions)
			if (this.result.exception ne null) {
				summaryBuilder.markAsFailed
			}
		}
		val summary = summaryBuilder.build

		//残余マナ分の料金等を、送信者に払い戻す。
		val payback = summary.calculateLeftOver + summary.calculateRefund
		Payment.txFee(this.track, this.tx.senderAddress, payback, Payment.TxFeeRefund)
		logger.info("<TxExecutor> Payed total refund to sender: %s. RefundVal=%,d. (ManaLeftOver=%,d. ManaRefund=%,d. EndMana=%,d)".format(
			this.tx.senderAddress, payback, summary.manaLeftOver, summary.manaRefund, this.endMana)
		)
		//採掘者に、採掘報酬を支払う。
		val txFee = summary.calculateFee
		Payment.txFee(this.track, this.coinbase, txFee, Payment.TxFee)
		logger.info("<TxExecutor> Payed fee to Miner[%s]; Fee=[%,d]; Block=[%,d]; Tx=[%s]".format(this.coinbase.toShortString, txFee, currentBlock.blockNumber, tx.hash.toShortString))

		Option(this.result).foreach {
			r => {
				this._logs = r.logsAsSeq
				r.deletedAccounts.foreach(address => this.track.delete(address.last20Bytes))
			}
		}
		this.listener.onTransactionExecuted(summary)
		if (NodeProperties.instance.vmTrace) {
			val trace = this.program.trace.result(this.result.hReturn).error(this.result.exception).toString
			saveProgramTrace(this.tx.hash.toHexString, trace)
			this.listener.onVMTraceCreated(this.tx.hash.toHexString, trace)
		}

		val endNanos = System.nanoTime
		val endTimestamp = System.currentTimeMillis
		txRecordLogger.info("%d,%s,%d,%d,%d".format(currentBlock.blockNumber, tx.hash, this.manaUsed, endNanos - this.startNanosRef.get, endTimestamp))
	}

	private def saveProgramTrace(txHash: String, content: String): Unit = {
		//TODO 未実装。
	}

	def logs: Seq[LogInfo] = this._logs
	def resultOption: Option[ProgramResult] = Option(this.result)
	def manaUsed: Long = this.tx.manaLimit.positiveBigInt.longValue() - this.endMana

}

object TransactionExecutor {
	private val logger = LoggerFactory.getLogger("execute")
	private val txRecordLogger = LoggerFactory.getLogger("tx_record")

	/**
	 * このトランザクションにかかるマナの量を返します。
	 */
	private def transactionCost(tx: TransactionLike): Long = {
		val nonZeroes = nonZeroDataBytes(tx)
		val zeroVals = tx.data.length - nonZeroes
		ManaCost.TRANSACTION + zeroVals * ManaCost.TX_ZERO_DATA + nonZeroes * ManaCost.TX_NO_ZERO_DATA
	}

	private def nonZeroDataBytes(tx: TransactionLike): Int = {
		if (ByteUtils.isNullOrEmpty(tx.data)) return 0
		tx.data.count(each => each != 0)
	}
}