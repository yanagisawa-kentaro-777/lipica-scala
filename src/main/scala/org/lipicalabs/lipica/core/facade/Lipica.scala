package org.lipicalabs.lipica.core.facade

import java.math.BigInteger
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.datastore.RepositoryLike
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.kernel.{Blockchain, CallTransaction, TransactionLike}
import org.lipicalabs.lipica.core.facade.listener.LipicaListener
import org.lipicalabs.lipica.core.net.endpoint.PeerClient
import org.lipicalabs.lipica.core.vm.program.ProgramResult

/**
 * 「自ノード」を定義する trait です。
 * 外部アプリケーションから自ノードへの操作や照会等は、
 * この trait の実装を通じて行うべきものです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
trait Lipica {

	/**
	 * 起動された日時を表すUNIX時刻（ミリ秒）を返します。
	 */
	def startupTimestamp: Long

	/**
	 * ノードを起動します。
	 */
	def startup(): Unit

	/**
	 * ノードを終了します。
	 * 終了したノードを再度起動することはできません。
	 */
	def shutdown(): Unit

	/**
	 * 自ノード内の重要なシングルトンコンポーネントのインスタンスを
	 * 保持する「配線基板」のインスタンスを返します。
	 */
	def componentsMotherboard: ComponentsMotherboard

	/**
	 * P2Pネットワークに接続可能なクライアントモジュールを返します。
	 */
	def client: PeerClient

	/**
	 * 自ノードのブロックチェーンを返します。
	 * （返されるインターフェイスを制限すべき。）
	 */
	def blockchain: Blockchain

	/**
	 * 自ノードの状態を管理するリポジトリを返します。
	 * （返されるインターフェイスを制限すべき。）
	 */
	def repository: RepositoryLike

	/**
	 * イベントリスナを追加登録します。
	 */
	def addListener(listener: LipicaListener): Unit

	/**
	 * 未確定のトランザクションの集合を返します。
	 */
	def pendingTransactions: Set[TransactionLike]

	/**
	 * 最近のトランザクションにおけるマナ価格の実績に基いて、
	 * おおむね妥当だと思われるマナ価格を計算して返します。
	 *
	 * 25%程度のトランザクションが、
	 * この価格かそれ以下で実行されている実績値です。
	 * より確実に優先的に実行してもらいたい場合には、20%程度割増すると良いでしょう。
	 */
	def recentManaPrice: Long

	/**
	 * 指定された番号のブロックを処理した後でノードの動作を停止します。
	 * （デバッグ用の機能です。）
	 */
	def exitOn(number: Long)

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
	def createTransaction(nonce: BigInteger, manaPrice: BigInteger, mana: BigInteger, receiveAddress: Array[Byte], value: BigInteger, data: Array[Byte]): TransactionLike

	/**
	 * トランザクションを呼び出します。
	 */
	def callConstantFunction(receiveAddress: String, function: CallTransaction.Function, funcArgs: Any*): Option[ProgramResult]

	def submitTransaction(tx: TransactionLike): Future[TransactionLike]

}

object Lipica {

	private val instanceRef = new AtomicReference[Lipica](null)

	def instance: Lipica = this.instanceRef.get

	/**
	 * 唯一のインスタンスを生成し、起動します。
	 * このメソッドを呼び出した後で、
	 * instance メソッドによってインスタンスを取得できるようになります。
	 */
	def startup(): Unit = {
		this.synchronized {
			if (Option(this.instanceRef.get).isDefined) {
				return
			}

			val result = new LipicaImpl
			result.startup()
			this.instanceRef.set(result)
			result
		}
	}

	/**
	 * 存在するインスタンスを終了させます。
	 *
	 * このメソッドを呼び出した後では、
	 * 再度 startup() を呼び出すことができます。
	 */
	def shutdown(): Unit = {
		this.synchronized {
			Option(this.instanceRef.get).foreach(_.shutdown())
			this.instanceRef.set(null)
		}
	}

}