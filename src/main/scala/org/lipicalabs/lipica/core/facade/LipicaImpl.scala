package org.lipicalabs.lipica.core.facade

import java.math.BigInteger
import java.net.InetSocketAddress
import java.util.concurrent.{Future, Executors}

import org.lipicalabs.lipica.core.concurrent.{ExecutorPool, CountingThreadFactory}
import org.lipicalabs.lipica.core.crypto.digest.Digest256
import org.lipicalabs.lipica.core.datastore.datasource.DataSourcePool
import org.lipicalabs.lipica.core.kernel.{Address160, CallTransaction, Transaction, TransactionLike}
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.listener.{LipicaListenerAdaptor, ManaPriceTracker, LipicaListener}
import org.lipicalabs.lipica.core.facade.components.{ComponentFactory, AdminInfo, ComponentsMotherboard}
import org.lipicalabs.lipica.core.net.endpoint.PeerClient
import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node, PeerInfo}
import org.lipicalabs.lipica.core.net.channel.ChannelManager
import org.lipicalabs.lipica.core.net.endpoint.PeerServer
import org.lipicalabs.lipica.core.facade.submit.{TransactionExecutor, TransactionTask}
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.program.ProgramResult
import org.lipicalabs.lipica.core.vm.program.context.ProgramContextFactory
import org.lipicalabs.lipica.utils.MiscUtils
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 19:40
 * YANAGISAWA, Kentaro
 */
class LipicaImpl extends Lipica {

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance
	def adminInfo: AdminInfo = componentsMotherboard.adminInfo
	def channelManager: ChannelManager = componentsMotherboard.channelManager
	val peerServer: PeerServer = new PeerServer
	def programContextFactory: ProgramContextFactory = componentsMotherboard.programContextFactory

	private val manaPriceTracker = new ManaPriceTracker

	override def init(): Unit = {
		val bindPort = NodeProperties.CONFIG.bindPort
		if (0 < bindPort) {
			val bindAddress = NodeProperties.CONFIG.bindAddress
			val socketAddress = new InetSocketAddress(bindAddress, bindPort)
			Executors.newSingleThreadExecutor(new CountingThreadFactory("front-server")).submit(new Runnable {
				override def run(): Unit = {
					peerServer.start(socketAddress)
				}
			})
		}
		addListener(this.manaPriceTracker)
	}

	override def findOnlinePeer(exclude: Set[PeerInfo]): Option[PeerInfo] = {
		this.componentsMotherboard.listener.trace("Looking for online peer.")
		this.componentsMotherboard.startPeerDiscovery()
		val peers = this.componentsMotherboard.peerDiscovery.peers
		peers.find(each => each.online && !exclude.contains(each))
	}

	/**
	 * Peerが発見されるまでブロックします。
	 */
	@tailrec
	override final def awaitOnlinePeer: PeerInfo = {
		val peerOption = findOnlinePeer(Set.empty)
		if (peerOption.isDefined) {
			return peerOption.get
		}
		Thread.sleep(100L)
		awaitOnlinePeer
	}

	override def getPeers: Set[PeerInfo] = this.componentsMotherboard.peerDiscovery.peers

	override def startPeerDiscovery() = this.componentsMotherboard.startPeerDiscovery()

	override def stopPeerDiscovery() = this.componentsMotherboard.stopPeerDiscovery()

	override def connect(node: Node) = connect(node.address, node.id)

	private val connectExecutor = ExecutorPool.instance.frontendConnector
	override def connect(address: InetSocketAddress, remoteNodeId: NodeId): Unit = {
		this.connectExecutor.submit(new Runnable {
			override def run(): Unit = {
				componentsMotherboard.client.connect(address, remoteNodeId)
			}
		})
	}

	override def callConstantFunction(receiveAddress: String, function: CallTransaction.Function, funcArgs: Any *): Option[ProgramResult] = {
		val tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L, receiveAddress, 0, function, funcArgs)
		tx.sign(ImmutableBytes.create(32))

		val bestBlock = componentsMotherboard.blockchain.bestBlock
		val executor = new org.lipicalabs.lipica.core.kernel.TransactionExecutor(
			tx, bestBlock.coinbase, componentsMotherboard.repository, componentsMotherboard.blockStore, programContextFactory, bestBlock, new LipicaListenerAdaptor, 0
		)
		executor.localCall = true

		executor.init()
		executor.execute()
		executor.go()
		executor.finalization()

		executor.resultOption
	}

	override def getBlockchain: BlockchainIF = new BlockchainIF(componentsMotherboard.blockchain)

	override def getAdminInfo: AdminInfo = componentsMotherboard.adminInfo

	override def getRepository: RepositoryIF = new RepositoryIF(componentsMotherboard.repository)

	override def addListener(listener: LipicaListener) = componentsMotherboard.listener.addListener(listener)

	override def client: PeerClient = this.componentsMotherboard.client


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

	override def submitTransaction(tx: TransactionLike): Future[TransactionLike] = {
		val task = new TransactionTask(tx, this.componentsMotherboard)
		TransactionExecutor.submitTransaction(task)
	}


	override def getSnapshotTo(root: Array[Byte]): RepositoryIF = {
		new RepositoryIF(this.componentsMotherboard.repository.createSnapshotTo(Digest256(root)))
	}

	override def getPendingTransactions: Set[TransactionLike] = this.componentsMotherboard.blockchain.pendingTransactions

	override def getChannelManager = this.componentsMotherboard.channelManager

	/**
	 * 最近のトランザクションにおけるマナ価格の実績に基いて、
	 * おおむね妥当だと思われるマナ価格を計算して返します。
	 *
	 * 25%程度のトランザクションが、
	 * この価格かそれ以下で実行されている実績値です。
	 * より確実に優先的に実行してもらいたい場合には、20%程度割増すると良いでしょう。
	 */
	override def getManaPrice = this.manaPriceTracker.getManaPrice

	override def close(): Unit = {
		this.componentsMotherboard.close()
	}

	override def exitOn(number: Long) = this.componentsMotherboard.blockchain.exitOn = number

	override def shutdown(): Unit = {
		import LipicaImpl._
		logger.info("<Lipica> Shutting down.")
		//動作中の ExecutorService 類を停止させる。
		ExecutorPool.instance.close()

		//DataSourceの始末をつける。
		val motherboard = ComponentsMotherboard.instance
		//Repository と BlockStore が flushされる。
		motherboard.blockchain.flush()
		//DataSourceをクローズする。
		DataSourcePool.closeAll()
		ComponentFactory.dataSources.values.foreach(MiscUtils.closeIfNotNull)

		logger.info("<Lipica> Shut down complete.")
	}
}

object LipicaImpl {
	private val logger = LoggerFactory.getLogger("general")
}