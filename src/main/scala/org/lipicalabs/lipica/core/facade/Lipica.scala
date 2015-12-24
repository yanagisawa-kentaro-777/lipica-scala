package org.lipicalabs.lipica.core.facade

import java.io.Closeable
import java.math.BigInteger
import java.net.InetSocketAddress
import java.util.concurrent.Future

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.manager.{BlockLoader, AdminInfo}
import org.lipicalabs.lipica.core.net.peer_discovery.PeerInfo
import org.lipicalabs.lipica.core.net.server.ChannelManager
import org.lipicalabs.lipica.core.net.transport.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
trait Lipica extends Closeable {

	def findOnlinePeer(exclude: java.util.Set[PeerInfo]): PeerInfo

	/**
	 * Peerが発見されるまでブロックします。
	 */
	def awaitOnlinePeer: PeerInfo

	def getPeers: Set[PeerInfo]

	def startPeerDiscovery(): Unit

	def stopPeerDiscovery(): Unit

	def connect(address: InetSocketAddress, remoteId: String)

	def connect(node: Node): Unit

	def getBlockchain: BlockchainIF

	def addListener(listener: LipicaListener): Unit

	def isConnected: Boolean

	override def close(): Unit

	/**
	 * Factory for general transaction
	 *
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

	def submitTransaction(tx: TransactionLike): Future[TransactionLike]

	def getRepository: RepositoryIF

	def init(): Unit

	def getSnapshotTo: RepositoryIF

	def getAdminInfo: AdminInfo

	def getChannelManager: ChannelManager

	def getPendingTransactions: java.util.Set[TransactionLike]

	def getBlockLoader: BlockLoader

	/**
	 * 最近のトランザクションにおけるマナ価格の実績に基いて、
	 * おおむね妥当だと思われるマナ価格を計算して返します。
	 *
	 * 25%程度のトランザクションが、
	 * この価格かそれ以下で実行されている実績値です。
	 * より確実に優先的に実行してもらいたい場合には、20%程度割増すると良いでしょう。
	 */
	def getManaPrice: Long

	def exitOn(number: Long)

}
