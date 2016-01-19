package org.lipicalabs.lipica.core.net.channel

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

import io.netty.channel.{ChannelHandlerContext, ChannelPipeline}
import io.netty.handler.timeout.ReadTimeoutHandler
import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.kernel.TransactionLike
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.handler.{Lpc, LpcAdaptor, LpcHandlerFactory}
import org.lipicalabs.lipica.core.net.lpc.message.LpcMessageFactory
import org.lipicalabs.lipica.core.sync.{SyncStateName, SyncStatistics}
import org.lipicalabs.lipica.core.net.message.{ImmutableMessages, MessageFactory}
import org.lipicalabs.lipica.core.net.p2p.{HelloMessage, P2PHandler, P2PMessageFactory}
import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, NodeStatistics, NodeManager, Node}
import org.lipicalabs.lipica.core.net.shh.{ShhHandler, ShhMessageFactory}
import org.lipicalabs.lipica.core.net.swarm.bzz.{BzzHandler, BzzMessageFactory}
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.lipicalabs.lipica.core.net.transport.{FrameCodec, MessageCodec}
import org.slf4j.LoggerFactory

/**
 * １個のピアとの間の伝送路を表すクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:55
 * YANAGISAWA, Kentaro
 */
class Channel {
	import Channel._

	private def worldManager: ComponentsMotherboard = ComponentsMotherboard.instance
	private def nodeManager: NodeManager = worldManager.nodeManager

	private val messageQueue: MessageQueue = new MessageQueue
	private val messageCodec: MessageCodec = new MessageCodec

	private val p2pHandler: P2PHandler = new P2PHandler(this.messageQueue)
	private val shhHandler: ShhHandler = new ShhHandler
	private val bzzHandler: BzzHandler = new BzzHandler

	private val lpcHandlerFactory = LpcHandlerFactory
	private val lpcRef: AtomicReference[Lpc] = new AtomicReference[Lpc](new LpcAdaptor)
	private def lpc: Lpc = this.lpcRef.get

	private val inetSocketAddressRef: AtomicReference[InetSocketAddress] = new AtomicReference[InetSocketAddress](null)
	def inetSocketAddress: InetSocketAddress = this.inetSocketAddressRef.get
	def inetSocketAddress_=(v: InetSocketAddress): Unit = this.inetSocketAddressRef.set(v)

	private val nodeRef: AtomicReference[Node] = new AtomicReference[Node](null)
	def node: Node = this.nodeRef.get
	def nodeId: NodeId = Option(this.node).map(_.id).getOrElse(NodeId.empty)
	def nodeIdShort: String = nodeId.toShortString

	/**
	 * このチャネルと、渡された識別子を持つノードとを結びつけます。
	 */
	def assignNode(nodeId: NodeId): Unit = {
		this.nodeRef.set(new Node(nodeId, this.inetSocketAddress))
		this._nodeStatistics = this.nodeManager.getNodeStatistics(this.node)
	}

	private var _nodeStatistics: NodeStatistics = null
	def nodeStatistics: NodeStatistics = this._nodeStatistics

	private var _discoveryMode: Boolean = false
	def isDiscoveryMode: Boolean = this._discoveryMode

	def init(pipeline: ChannelPipeline, remoteNodeId: NodeId, aDiscoveryMode: Boolean): Unit = {
		pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(NodeProperties.CONFIG.readTimeoutMillis, TimeUnit.MILLISECONDS))
		pipeline.addLast("initiator", messageCodec.initiator)
		pipeline.addLast("messageCodec", messageCodec)

		this._discoveryMode = aDiscoveryMode

		if (aDiscoveryMode) {
			//reputationに影響しないように、匿名化する。
			this.messageCodec.generateTempKey()
		}
		this.messageCodec.setRemoteNodeId(remoteNodeId, this)

		this.p2pHandler.messageQueue = this.messageQueue
		this.messageCodec.setP2PMessageFactory(new P2PMessageFactory)

		this.shhHandler.messageQueue = this.messageQueue
		this.shhHandler.privateKey = NodeProperties.CONFIG.privateKey
		this.messageCodec.setShhMessageFactory(new ShhMessageFactory)

		this.bzzHandler.messageQueue = this.messageQueue
		this.messageCodec.setBzzMessageFactory(new BzzMessageFactory)
	}

	def publicTransportHandshakeFinished(ctx: ChannelHandlerContext, helloMessage: HelloMessage): Unit = {
		ctx.pipeline.addLast(Capability.P2P, this.p2pHandler)
		this.p2pHandler.channel = this
		this.p2pHandler.setHandshake(helloMessage, ctx)

		nodeStatistics.transportHandshake.add
	}

	def sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: NodeId): Unit = {
		//discovery modeでは、外部からの接続を受け付けないために嘘のポート番号を供給する。
		val helloMessage = if (this._discoveryMode) ImmutableMessages.createHelloMessage(nodeId, 9) else ImmutableMessages.createHelloMessage(nodeId)
		val payload = helloMessage.toEncodedBytes

		val byteBuffer = ctx.alloc.buffer
		frameCodec.writeFrame(new Frame(helloMessage.code, payload.toByteArray), byteBuffer)
		ctx.writeAndFlush(byteBuffer).sync()
		logger.info("<Channel> To: %s, Sent: %s".format(ctx.channel.remoteAddress, helloMessage))
		nodeStatistics.transportOutHello.add
	}

	def activateLpc(ctx: ChannelHandlerContext, version: LpcVersion): Unit = {
		val handler = this.lpcHandlerFactory.create(version)
		val messageFactory = createLpcMessageFactory(version)
		this.messageCodec.setLpcMessageFactory(messageFactory)

		logger.info("<Channel> Lpc %s: [address=%s, id=%s]".format(handler.version, this.inetSocketAddress, nodeId.toShortString))

		ctx.pipeline.addLast(Capability.LPC, handler)
		handler.messageQueue = this.messageQueue
		handler.channel = this
		handler.peerDiscoveryMode = this._discoveryMode

		handler.activate()

		this.lpcRef.set(handler)
	}

	private def createLpcMessageFactory(version: LpcVersion): MessageFactory = {
		new LpcMessageFactory
	}

	def activateShh(ctx: ChannelHandlerContext): Unit = {
		ctx.pipeline.addLast(Capability.SHH, this.shhHandler)
		this.shhHandler.activate()
	}

	def activateBzz(ctx: ChannelHandlerContext): Unit = {
		ctx.pipeline.addLast(Capability.BZZ, this.bzzHandler)
		this.bzzHandler.activate()
	}

	def initMessageCodes(capabilities: Seq[Capability]): Unit = {
		this.messageCodec.initMessageCodes(capabilities)
	}

	def isProtocolInitialized: Boolean = this.lpc.hasStatusPassed

	def onDisconnect(): Unit = this.lpc.onShutdown()

	def onSyncDone(): Unit = {
		this.lpc.enableTransactions()
		this.lpc.onSyncDone()
	}

	def hasLpcStatusSucceeded: Boolean = this.lpc.hasStatusSucceeded

	def logSyncStats(): Unit = this.lpc.logSyncStats()

	def totalDifficulty: BigInt = this.nodeStatistics.lpcTotalDifficulty

	def changeSyncState(state: SyncStateName): Unit = this.lpc.changeState(state)

	def hasBlocksLack: Boolean = this.lpc.hasBlocksLack

	def maxHashesAsk: Int = this.lpc.maxHashesAsk
	def maxHashToAsk_=(v: Int): Unit = this.lpc.maxHashesAsk = v

	def lastHashToAsk: DigestValue = this.lpc.lastHashToAsk
	def lastHashToAsk_=(v: DigestValue): Unit = this.lpc.lastHashToAsk = v

	def bestKnownHash: DigestValue = this.lpc.bestKnownHash

	def getSyncStats: SyncStatistics = this.lpc.getSyncStats

	def isIdle: Boolean = this.lpc.isIdle
	def isHashRetrieving: Boolean = this.lpc.isHashRetrieving
	def isHashRetrievingDone: Boolean = this.lpc.isHashRetrievingDone

	def sendTransaction(tx: TransactionLike): Unit = this.lpc.sendTransaction(tx)

	def lpcVersion: LpcVersion = this.lpc.version

	def syncState: SyncStateName = this.lpc.getSyncState

	def syncStats: SyncStatistics = this.lpc.getSyncStats

	def syncStateSummaryAsString: String =  this.lpc.syncStateSummaryAsString

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[Channel]
			if (this.node eq another.node) {
				true
			} else {
				this.node == another.node
			}
		} catch {
			case e: Throwable => false
		}
	}

	override def hashCode: Int = Option(this.node).map(_.hashCode).getOrElse(0)

}

object Channel {
	private val logger = LoggerFactory.getLogger("net")
}