package org.lipicalabs.lipica.core.datastore

import java.io.Closeable

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.{Address, Block, AccountState, ContractDetails}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord

import scala.collection.mutable

/**
 * 残高、コントラクトのコード、コントラクトのストレージ等、
 * グローバルな状態を保存するクラスが実装すべき trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 13:46
 * YANAGISAWA, Kentaro
 */
trait RepositoryLike extends Closeable {

	/**
	 * アカウントを作成します。
	 * @param address 作成対象のアドレス。
	 * @return 作成されたアカウントの状態。
	 */
	def createAccount(address: Address): AccountState

	/**
	 * アカウントの存否確認を行います。
	 * @param address 検査対象のアカウント。
	 */
	def existsAccount(address: Address): Boolean

	/**
	 * アカウントを取得します。
	 */
	def getAccountState(address: Address): Option[AccountState]

	/**
	 * アカウントを削除します。
	 */
	def delete(address: Address): Unit

	/**
	 * 指定されたアカウントのnonceを１増やします。
	 * （アカウントが存在しなかったら作成します。）
	 */
	def increaseNonce(address: Address): BigInt

	/**
	 * 指定されたアカウントの現在のnonceを返します。
	 * （アカウントが存在しなかったら作成します。）
	 */
	def getNonce(address: Address): BigInt

	/**
	 * 指定されたアカウントに対応するコントラクト明細を取得して返します。
	 */
	def getContractDetails(address: Address): Option[ContractDetails]

	/**
	 * 指定されたアカウントに対して、コードを保存します。
	 */
	def saveCode(address: Address, code: ImmutableBytes): Unit

	/**
	 * 指定されたアカウントに結び付けられたコードを読み取ります。
	 */
	def getCode(address: Address): Option[ImmutableBytes]

	/**
	 * 一括更新し、渡された可変な連想配列を消去します。
	 */
	def updateBatch(accountStates: mutable.Map[Address, AccountState], contractDetails: mutable.Map[Address, ContractDetails]): Unit

	/**
	 * 渡されたアドレスに関する情報を一括ロードし、渡された可変な連想配列に書き込みます。
	 */
	def loadAccount(address: Address, cacheAccounts: mutable.Map[Address, AccountState], cacheDetails: mutable.Map[Address, ContractDetails]): Unit

	/**
	 * 指定されたアカウントに対して、キーと値の組み合わせを登録します。
	 */
	def addStorageRow(address: Address, key: VMWord, value: VMWord): Unit

	/**
	 * 指定されたアカウントにおいて、キーに対応する値を取得して返します。
	 */
	def getStorageValue(address: Address, key: VMWord): Option[VMWord]

	/**
	 * 指定されたアカウントにおいて、キー（複数）に対応する「キー・値ペアの連想配列」を取得して返します。
	 */
	def getStorageContent(address: Address, keys: Iterable[VMWord]): Map[VMWord, VMWord]

	/**
	 * 指定されたアカウントの残高を返します。
	 */
	def getBalance(address: Address): Option[BigInt]

	/**
	 * 指定されたアカウントの残高に、指定された値を足します。
	 */
	def addBalance(address: Address, value: BigInt): BigInt

	/**
	 * 管理対象アドレスすべての集合を返します。
	 * （実用性は疑問。限定されたテスト用。）
	 */
	def getAccountKeys: Set[Address]

	/**
	 * 迅速に更新処理を行うためのバッファを生成して返します。
	 */
	def startTracking: RepositoryTrackLike

	def dumpState(block: Block, gasUsed: Long, txNumber: Int, txHash: ImmutableBytes): Unit

}

/**
 * 永続化可能なRepository本体を表す trait です。
 */
trait Repository extends RepositoryLike {

	/**
	 * このオブジェクトの現在のルートハッシュを返します。
	 */
	def rootHash: DigestValue

	/**
	 * このオブジェクトを、渡されたルートハッシュの状態に巻き戻します。
	 */
	def syncToRoot(root: DigestValue)

	/**
	 * 渡されたルートハッシュの状態に対応するオブジェクトを新たに生成して返します。
	 * @param root ルートハッシュ値。
	 * @return 生成されたオブジェクト。
	 */
	def createSnapshotTo(root: DigestValue): Repository

	/**
	 * 永続化します。
	 */
	def flush(): Unit

	/**
	 * 永続化します。
	 */
	def flushNoReconnect(): Unit

	/**
	 * このオブジェクトの内部状態を再初期化します。
	 */
	def reset(): Unit

	/**
	 * 動作を停止させます。
	 */
	def close(): Unit

	/**
	 * クローズ状態であるか否かを返します。
	 * @return クローズ状態であれば真。
	 */
	def isClosed: Boolean

}

/**
 * Repository本体に対して確定する前の読み書きを、
 * 迅速に実行するためのバッファを表す trait です。
 */
trait RepositoryTrackLike extends RepositoryLike {

	/**
	 * 変更を確定します。（Trackのみの操作。）
	 */
	def commit(): Unit

	/**
	 * 変更を巻き戻します。（Trackのみの操作。）
	 */
	def rollback(): Unit

	/**
	 * この Track が依拠する元の RepositoryLike を返します。
	 */
	def originalRepository: RepositoryLike

}