package org.lipicalabs.lipica.core.net.p2p

import java.net.{InetAddress, InetSocketAddress}
import java.util.concurrent._

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcVersion
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode._
import org.lipicalabs.lipica.core.net.peer_discovery.PeerInfo
import org.lipicalabs.lipica.core.net.channel.{MessageQueue, Channel}
import org.lipicalabs.lipica.core.net.shh.ShhHandler
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzHandler
import org.lipicalabs.lipica.core.net.transport.HandshakeHelper
import org.lipicalabs.lipica.core.utils.ErrorLogger
import org.slf4j.LoggerFactory

/**
 * 暗号化された通信経路の維持管理に関連するメッセージのハンドラクラスです。
 *
 * Created by IntelliJ IDEA.
 * @since 2015/12/07 20:32
 * @author YANAGISAWA, Kentaro
 */
class P2PHandler(private var _messageQueue: MessageQueue) extends SimpleChannelInboundHandler[P2PMessage] {

	import P2PHandler._

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v

	private var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	private var _handshakeHelloMessage: HelloMessage = null
	def handshakeHelloMessage: HelloMessage = this._handshakeHelloMessage

	private var _channel: Channel = null
	def channel: Channel = this._channel
	def channel_=(v: Channel): Unit = this._channel = v

	private var pingTask: ScheduledFuture[_] = null

	/**
	 * 通信開始イベント。
	 */
	override def handlerAdded(ctx: ChannelHandlerContext): Unit = {
		logger.info("<P2PHandler> P2P protocol activated.")
		this.messageQueue.activate(ctx)
		this.componentsMotherboard.listener.trace("P2P protocol activated.")
		startTimers()
	}

	/**
	 * メッセージの受信イベント。
	 */
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
				onHandshakeDone(message.asInstanceOf[HelloMessage], ctx)
			case Disconnect =>
				this.messageQueue.receiveMessage(message)
				this.channel.nodeStatistics.nodeDisconnectedRemote(message.asInstanceOf[DisconnectMessage].reason)
			case Ping =>
				this.messageQueue.receiveMessage(message)
				ctx.writeAndFlush(P2PMessageFactory.PongMessage)
			case Pong =>
				this.messageQueue.receiveMessage(message)
			case _ =>
				ctx.fireChannelRead(message)
		}
	}

	/**
	 * 無通信検知イベント。
	 */
	override def channelInactive(ctx: ChannelHandlerContext): Unit = {
		logger.info("<P2PHandler> Channel inactive: %s".format(ctx))
		this.killTimers()
	}

	/**
	 * 例外捕捉イベント。
	 */
	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		ErrorLogger.logger.warn("<P2PHandler> Error caught: %s".format(cause))
		logger.warn("<P2PHandler> Error caught: %s".format(cause))
		ctx.close()
		killTimers()
	}

	private def disconnect(reasonCode: ReasonCode): Unit = {
		this.messageQueue.sendMessage(DisconnectMessage(reasonCode))
		this.channel.nodeStatistics.nodeDisconnectedLocal(reasonCode)
	}

	/**
	 * 伝送路の確立とハンドシェイクメッセージの授受が完了した際に呼び出されます。
	 */
	def onHandshakeDone(message: HelloMessage, ctx: ChannelHandlerContext): Unit = {
		this.channel.nodeStatistics.clientId = message.clientId

		this._handshakeHelloMessage = message
		if (message.p2pVersion != Version) {
			disconnect(ReasonCode.IncompatibleProtocol)
		} else {
			//両ノードが共有するプロトコルを有効化する。
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
			//ハンドシェイクが完了したピアの情報をコールバックする。
			val address: InetAddress = ctx.channel().remoteAddress().asInstanceOf[InetSocketAddress].getAddress
			val port = message.listenPort
			val confirmedPeer = new PeerInfo(new InetSocketAddress(address, port), message.nodeId)
			confirmedPeer.online = false
			confirmedPeer.addCapabilities(message.capabilities)

			this.componentsMotherboard.peerDiscovery.addPeer(confirmedPeer)
			this.componentsMotherboard.listener.onHandshakePeer(channel.node, message)
		}
	}

	private def startTimers(): Unit = {
		this.pingTask = pingTimer.scheduleAtFixedRate(
			new Runnable {
				override def run(): Unit = {
					messageQueue.sendMessage(P2PMessageFactory.PingMessage)
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