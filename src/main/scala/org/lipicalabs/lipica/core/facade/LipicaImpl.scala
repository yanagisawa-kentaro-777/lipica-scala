package org.lipicalabs.lipica.core.facade

import java.math.BigInteger
import java.net.InetSocketAddress
import java.util.concurrent.{Future, Executors}

import org.lipicalabs.lipica.core.kernel.CallTransaction
import org.lipicalabs.lipica.core.kernel.{Transaction, TransactionLike}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.facade.listener.{LipicaListenerAdaptor, ManaPriceTracker, LipicaListener}
import org.lipicalabs.lipica.core.facade.components.{AdminInfo, ComponentsMotherboard}
import org.lipicalabs.lipica.core.net.endpoint.PeerClient
import org.lipicalabs.lipica.core.net.peer_discovery.{Node, PeerInfo}
import org.lipicalabs.lipica.core.net.channel.ChannelManager
import org.lipicalabs.lipica.core.net.endpoint.PeerServer
import org.lipicalabs.lipica.core.facade.submit.{TransactionExecutor, TransactionTask}
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.program.ProgramResult
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeFactory

import scala.annotation.tailrec

/**
 * Created by IntelliJ IDEA.
 * 2015/12/25 19:40
 * YANAGISAWA, Kentaro
 */
class LipicaImpl extends Lipica {

	private def worldManager: ComponentsMotherboard = ComponentsMotherboard.instance
	def adminInfo: AdminInfo = worldManager.adminInfo
	def channelManager: ChannelManager = worldManager.channelManager
	val peerServer: PeerServer = new PeerServer
	def programInvokeFactory: ProgramInvokeFactory = worldManager.programInvokeFactory

	private val manaPriceTracker = new ManaPriceTracker

	override def init(): Unit = {
		val bindPort = SystemProperties.CONFIG.bindPort
		if (0 < bindPort) {
			val bindAddress = SystemProperties.CONFIG.bindAddress
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
		this.worldManager.listener.trace("Looking for online peer.")
		this.worldManager.startPeerDiscovery()
		val peers = this.worldManager.peerDiscovery.peers
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

	override def getPeers: Set[PeerInfo] = this.worldManager.peerDiscovery.peers

	override def startPeerDiscovery() = this.worldManager.startPeerDiscovery()

	override def stopPeerDiscovery() = this.worldManager.stopPeerDiscovery()

	override def connect(node: Node) = connect(node.address, node.id)

	private val connectExecutor = Executors.newCachedThreadPool(new CountingThreadFactory("front-connector"))
	override def connect(address: InetSocketAddress, remoteNodeId: ImmutableBytes): Unit = {
		this.connectExecutor.submit(new Runnable {
			override def run(): Unit = {
				worldManager.client.connect(address, remoteNodeId)
			}
		})
	}

	override def callConstantFunction(receiveAddress: String, function: CallTransaction.Function, funcArgs: Any *): Option[ProgramResult] = {
		val tx = CallTransaction.createCallTransaction(0, 0, 100000000000000L, receiveAddress, 0, function, funcArgs)
		tx.sign(ImmutableBytes.create(32))

		val bestBlock = worldManager.blockchain.bestBlock
		val executor = new org.lipicalabs.lipica.core.kernel.TransactionExecutor(
			tx, bestBlock.coinbase, worldManager.repository, worldManager.blockStore, programInvokeFactory, bestBlock, new LipicaListenerAdaptor, 0
		)
		executor.localCall = true

		executor.init()
		executor.execute()
		executor.go()
		executor.finalization()

		executor.resultOption
	}

	override def getBlockchain: BlockchainIF = new BlockchainIF(worldManager.blockchain)

	override def getAdminInfo: AdminInfo = worldManager.adminInfo

	override def getRepository: RepositoryIF = new RepositoryIF(worldManager.repository)

	override def addListener(listener: LipicaListener) = worldManager.listener.addListener(listener)

	override def client: PeerClient = this.worldManager.client


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
		val nonceBytes = ImmutableBytes.asUnsignedByteArray(nonce)
		val manaPriceBytes = ImmutableBytes.asUnsignedByteArray(manaPrice)
		val manaBytes = ImmutableBytes.asUnsignedByteArray(mana)
		val valueBytes = ImmutableBytes.asUnsignedByteArray(value)

		Transaction(nonceBytes, manaPriceBytes, manaBytes, ImmutableBytes(receiveAddress), valueBytes, ImmutableBytes(data))
	}

	override def submitTransaction(tx: TransactionLike): Future[TransactionLike] = {
		val task = new TransactionTask(tx, this.worldManager)
		TransactionExecutor.submitTransaction(task)
	}


	override def getSnapshotTo(root: Array[Byte]): RepositoryIF = {
		new RepositoryIF(this.worldManager.repository.createSnapshotTo(ImmutableBytes(root)))
	}

	override def getPendingTransactions: Set[TransactionLike] = this.worldManager.blockchain.pendingTransactions

	override def getChannelManager = this.worldManager.channelManager

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
		this.worldManager.close()
	}

	override def exitOn(number: Long) = this.worldManager.blockchain.exitOn = number

}
