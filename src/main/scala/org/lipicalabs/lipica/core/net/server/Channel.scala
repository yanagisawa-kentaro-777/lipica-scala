package org.lipicalabs.lipica.core.net.server

import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

import io.netty.channel.{ChannelPipeline, ChannelHandlerContext}
import io.netty.handler.timeout.ReadTimeoutHandler
import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.lpc.handler.{Lpc, LpcAdaptor, LpcHandlerFactory}
import org.lipicalabs.lipica.core.net.lpc.message.LpcMessageFactory
import org.lipicalabs.lipica.core.net.lpc.sync.{SyncStatistics, SyncStateName}
import org.lipicalabs.lipica.core.net.message.{MessageFactory, ImmutableMessages}
import org.lipicalabs.lipica.core.net.p2p.{P2PMessageFactory, P2PHandler, HelloMessage}
import org.lipicalabs.lipica.core.net.shh.{ShhMessageFactory, ShhHandler}
import org.lipicalabs.lipica.core.net.swarm.bzz.{BzzMessageFactory, BzzHandler}
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.lipicalabs.lipica.core.net.transport.{MessageCodec, FrameCodec, Node}
import org.lipicalabs.lipica.core.net.transport.discover.{NodeManager, NodeStatistics}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:55
 * YANAGISAWA, Kentaro
 */
class Channel {
	import Channel._

	//TODO auto wiring
	private def worldManager: WorldManager = WorldManager.instance

	private val messageQueue: MessageQueue = ???
	private val p2pHandler: P2PHandler = ???
	private val shhHandler: ShhHandler = ???
	private val bzzHandler: BzzHandler = ???
	private val messageCodec: MessageCodec = ???

	private def nodeManager: NodeManager = worldManager.nodeManager

	private val lpcHandlerFactory = LpcHandlerFactory
	private var lpc: Lpc = new LpcAdaptor

	private var _inetSocketAddress: InetSocketAddress = null
	def inetSocketAddress: InetSocketAddress = this._inetSocketAddress
	def inetSocketAddress_=(v: InetSocketAddress): Unit = this._inetSocketAddress = v

	private var _node: Node = null
	def node: Node = this._node
	def nodeId: ImmutableBytes = Option(this.node).map(_.id).orNull
	def peerId: String = Option(this.node).map(_.hexId).getOrElse("null")
	def peerIdShort: String = Option(this.node).map(_.hexIdShort).getOrElse("null")
	def setNode(nodeId: ImmutableBytes): Unit = {
		this._node = new Node(nodeId, this.inetSocketAddress.getHostName, this.inetSocketAddress.getPort)
		this._nodeStatistics = this.nodeManager.getNodeStatistics(this._node)
	}

	private var _nodeStatistics: NodeStatistics = null
	def nodeStatistics: NodeStatistics = this._nodeStatistics

	private var _discoveryMode: Boolean = false
	def isDiscoveryMode: Boolean = this._discoveryMode

	def init(pipeline: ChannelPipeline, remoteId: String, aDiscoveryMode: Boolean): Unit = {
		pipeline.addLast("readTimeoutHandler", new ReadTimeoutHandler(SystemProperties.CONFIG.peerChannelReadTimeoutSeconds, TimeUnit.SECONDS))
		pipeline.addLast("initiator", messageCodec.initiator)
		pipeline.addLast("messageCodec", messageCodec)

		this._discoveryMode = aDiscoveryMode

		if (aDiscoveryMode) {
			//reputationに影響しないように、匿名化する。
			this.messageCodec.generateTempKey()
		}
		this.messageCodec.setRemoteId(remoteId, this)

		this.p2pHandler.messageQueue = this.messageQueue
		this.messageCodec.setP2PMessageFactory(new P2PMessageFactory)

		this.shhHandler.messageQueue = this.messageQueue
		this.shhHandler.privateKey = SystemProperties.CONFIG.myKey
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

	def sendHelloMessage(ctx: ChannelHandlerContext, frameCodec: FrameCodec, nodeId: String): Unit = {
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

		logger.info("<Channel> Lpc %s: [address=%s, id=%s]".format(handler.version, this._inetSocketAddress, peerIdShort))

		ctx.pipeline.addLast(Capability.LPC, handler)
		handler.messageQueue = this.messageQueue
		handler.channel = this
		handler.peerDiscoveryMode = this._discoveryMode

		handler.activate()

		this.lpc = handler
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

	def lastHashToAsk: ImmutableBytes = this.lpc.lastHashToAsk
	def lastHashToAsk_=(v: ImmutableBytes): Unit = this.lpc.lastHashToAsk = v

	def bestKnownHash: ImmutableBytes = this.lpc.bestKnownHash

	def getSyncStats: SyncStatistics = this.lpc.getSyncStats

	def isIdle: Boolean = this.lpc.isIdle
	def isHashRetrieving: Boolean = this.lpc.isHashRetrieving
	def isHashRetrievingDone: Boolean = this.lpc.isHashRetrievingDone

	def sendTransaction(tx: TransactionLike): Unit = this.lpc.sendTransaction(tx)

	def lpcVersion: LpcVersion = this.lpc.version

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