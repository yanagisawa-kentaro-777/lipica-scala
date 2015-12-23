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
	 * Calculates a 'reasonable' Gas price based on statistics of the latest transaction's Gas prices
	 * Normally the price returned should be sufficient to execute a transaction since ~25% of the latest
	 * transactions were executed at this or lower price.
	 * If the transaction is wanted to be executed promptly with higher chances the returned price might
	 * be increased at some ratio (e.g. * 1.2)
	 */
	def getManaPrice: Long

	def exitOn(number: Long)

}
