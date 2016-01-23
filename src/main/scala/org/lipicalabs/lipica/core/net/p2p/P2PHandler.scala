package org.lipicalabs.lipica.core.net.p2p

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent._

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.message.{ReasonCode, ImmutableMessages}
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode._
import org.lipicalabs.lipica.core.net.peer_discovery.PeerInfo
import org.lipicalabs.lipica.core.net.channel.{MessageQueue, Channel}
import org.lipicalabs.lipica.core.net.shh.ShhHandler
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzHandler
import org.lipicalabs.lipica.core.net.transport.HandshakeHelper
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/07 20:32
 * YANAGISAWA, Kentaro
 */
class P2PHandler(private var _messageQueue: MessageQueue) extends SimpleChannelInboundHandler[P2PMessage] {

	import P2PHandler._

	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v

	private var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	private var _handshakeHelloMessage: HelloMessage = null
	def handshakeHelloMessage: HelloMessage = this._handshakeHelloMessage

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private var _channel: Channel = null
	def channel: Channel = this._channel
	def channel_=(v: Channel): Unit = this._channel = v

	private var pingTask: ScheduledFuture[_] = null

	override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
		logger.info("<P2PHandler> P2P protocol activated.")
		this.messageQueue.activate(ctx)
		this.componentsMotherboard.listener.trace("P2P protocol activated.")
		startTimers()
	}

	override def channelRead0(ctx: ChannelHandlerContext, message: P2PMessage): Unit = {
		if (P2PMessageCode.inRange(message.command.asByte)) {
			if (logger.isDebugEnabled) {
				logger.debug("<P2PHandler> Received: %s".format(message.command))
			}
		}
		this.componentsMotherboard.listener.trace("<P2PHandler> P2PHandler invoked: %s".format(message.command))
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
		ErrorLogger.logger.warn("<P2PHandler> Error caught: %s".format(cause))
		logger.warn("<P2PHandler> Error caught: %s".format(cause))
		ctx.close()
		killTimers()
	}

	def setHandshake(message: HelloMessage, ctx: ChannelHandlerContext): Unit = {
		this.channel.nodeStatistics.clientId = message.clientId

		this._handshakeHelloMessage = message
		if (message.p2pVersion != Version) {
			disconnect(ReasonCode.IncompatibleProtocol)
		} else {
			val capsInCommon = HandshakeHelper.getSupportedCapabilities(message)
			this.channel.initMessageCodes(capsInCommon)
			for (capability <- capsInCommon) {
				if ((capability.name == Capability.LPC) && LpcVersion.isSupported(capability.version)) {
					this.channel.activateLpc(ctx, LpcVersion.fromCode(capability.version).get)
				} else if ((capability.name == Capability.SHH) && (capability.version == ShhHandler.Version)) {
					channel.activateShh(ctx)
				} else if ((capability.name == Capability.BZZ) && (capability.version == BzzHandler.Version)) {
					channel.activateBzz(ctx)
				}
			}
			val address: InetAddress = ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress].getAddress
			val port = message.listenPort
			val confirmedPeer = new PeerInfo(new InetSocketAddress(address, port), message.nodeId)
			confirmedPeer.online = false
			confirmedPeer.addCapabilities(message.capabilities)

			this.componentsMotherboard.peerDiscovery.addPeer(confirmedPeer)
			this.componentsMotherboard.listener.onHandshakePeer(channel.node, message)
		}
	}

//	def sendTransaction(tx: TransactionLike): Unit = {
//		val message = new TransactionsMessage(Seq(tx))
//		this.messageQueue.sendMessage(message)
//	}
//
//	def sendNewBlock(block: Block): Unit = {
//		val message = new NewBlockMessage(block, block.difficulty)
//		this.messageQueue.sendMessage(message)
//	}

//	def sendDisconnect(): Unit =  this.messageQueue.disconnect()

	private def startTimers(): Unit = {
		this.pingTask = pingTimer.scheduleAtFixedRate(
			new Runnable {
				override def run(): Unit = {
					messageQueue.sendMessage(ImmutableMessages.PingMessage)
				}
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

	private val pingTimer: ScheduledExecutorService = ExecutorPool.instance.p2pHandlerProcessor
}