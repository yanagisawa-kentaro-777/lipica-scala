package org.lipicalabs.lipica.core.net.p2p

import java.util.concurrent._

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.base.{Block, TransactionLike}
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.message.{NewBlockMessage, TransactionsMessage}
import org.lipicalabs.lipica.core.net.message.{ReasonCode, ImmutableMessages}
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode._
import org.lipicalabs.lipica.core.net.peer_discovery.PeerInfo
import org.lipicalabs.lipica.core.net.server.Channel
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:32
 * YANAGISAWA, Kentaro
 */
class P2PHandler(private val messageQueue: MessageQueue) extends SimpleChannelInboundHandler[P2PMessage] {

	import P2PHandler._

	private var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	private var _handshakeHelloMessage: HelloMessage = null
	def handshakeHelloMessage: HelloMessage = this._handshakeHelloMessage

	private var lastPeersSent: Set[PeerInfo] = null

	//TODO auto wiring
	private var _worldManager: WorldManager = ???
	def worldManager: WorldManager = this._worldManager
	def worldManager_=(v: WorldManager): Unit = this._worldManager = v

	private var _channel: Channel = null
	def channel: Channel = this._channel
	def channel_=(v: Channel): Unit = this._channel = v

	private var pingTask: ScheduledFuture[_] = null

	override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
		logger.info("<P2PHandler> P2P protocol activated.")
		this.messageQueue.activate(ctx)
		this.worldManager.listener.trace("P2P protocol activated.")
		startTimers()
	}

	override def channelRead0(ctx: ChannelHandlerContext, message: P2PMessage): Unit = {
		if (P2PMessageCode.inRange(message.command.asByte)) {
			if (logger.isTraceEnabled) {
				logger.trace("<P2PHandler> P2PHandler invoked: %s".format(message.command))
			}
		}
		this.worldManager.listener.trace("<P2PHandler> P2PHandler invoked: %s".format(message.command))
		message.command match {
			case Hello =>
				this.messageQueue.receiveMessage(message)
				setHandshake(message.asInstanceOf[HelloMessage], ctx)
			case Disconnect =>
				this.messageQueue.receiveMessage(message)
				this.channel.nodeStatistics.nodeDisconnectedRemote(message.asInstanceOf[DisconnectMessage].reason)
			case Ping =>
				this.messageQueue.receiveMessage(message)
				ctx.writeAndFlush(ImmutableMessages.PongMessage)
			case Pong =>
				this.messageQueue.receiveMessage(message)
			case GetPeers =>
				this.messageQueue.receiveMessage(message)
				sendPeers()
			case Peers =>
				this.messageQueue.receiveMessage(message)
				processPeers(ctx, message.asInstanceOf[PeersMessage])

				if (this.peerDiscoveryMode || !handshakeHelloMessage.capabilities.contains(Capability.LPC)) {
					disconnect(ReasonCode.Requested)
					killTimers()
					ctx.close().sync()
					ctx.disconnect().sync()
				}
			case _ =>
				ctx.fireChannelRead(message)
		}
	}

	private def disconnect(reasonCode: ReasonCode): Unit = {
		this.messageQueue.sendMessage(DisconnectMessage(reasonCode))
		this.channel.nodeStatistics.nodeDisconnectedLocal(reasonCode)
	}

	override def channelInactive(ctx: ChannelHandlerContext): Unit = {
		logger.info("<P2PHandler> Channel inactive: %s".format(ctx))
		this.killTimers()
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		logger.warn("<P2PHandler> Error caught: %s".format(cause))
		ctx.close()
		killTimers()
	}

	private def processPeers(ctx: ChannelHandlerContext, peersMessage: PeersMessage): Unit = {
		this.worldManager.peerDiscovery.addPeers(peersMessage.peers)
	}

	private def sendGetPeers(): Unit = {
		this.messageQueue.sendMessage(ImmutableMessages.GetPeersMessage)
	}

	private def sendPeers(): Unit = {
		val peers = this.worldManager.peerDiscovery.peers
		if ((this.lastPeersSent ne null) && (this.lastPeersSent == peers)) {
			logger.info("<P2PHandler> No new peers discovered. Do not answer GetPeers.")
			return
		}
		val peerSet = peers.map(each => new Peer(each.address, each.port, each.peerId, Seq.empty)).toSet
		val message = PeersMessage(peerSet)
		this.lastPeersSent = peers
		this.messageQueue.sendMessage(message)
	}

	def setHandshake(message: HelloMessage, ctx: ChannelHandlerContext): Unit = {
		//TODO 未実装。
	}

	def sendTransaction(tx: TransactionLike): Unit = {
		val message = new TransactionsMessage(Seq(tx))
		this.messageQueue.sendMessage(message)
	}

	def sendNewBlock(block: Block): Unit = {
		val message = new NewBlockMessage(block, block.difficulty)
		this.messageQueue.sendMessage(message)
	}

	def sendDisconnect(): Unit =  this.messageQueue.disconnect()

	def adaptMessageIds(capabilities: Iterable[Capability]): Unit = {
		//TODO 未実装。
	}

	private def startTimers(): Unit = {
		this.pingTask = pingTimer.scheduleAtFixedRate(
			new Runnable {
				override def run() = messageQueue.sendMessage(ImmutableMessages.PingMessage)
			},
			2, 5, TimeUnit.SECONDS
		)
	}

	private def killTimers(): Unit = {
		this.pingTask.cancel(false)
		this.messageQueue.close()
	}

}

object P2PHandler {
	private val logger = LoggerFactory.getLogger("net")
	val Version: Byte = 4

	private val pingTimer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory {
		override def newThread(r: Runnable) = new Thread(r, "P2PPingTimer")
	})

}