package org.lipicalabs.lipica.core.net.transport

import java.net.InetSocketAddress
import java.util

import com.google.common.io.ByteStreams
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.handler.codec.ByteToMessageCodec
import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.crypto.{ECIESCoder, ECKey}
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.{Command, MessageFactory, Message}
import org.lipicalabs.lipica.core.net.p2p.{P2PMessageFactory, DisconnectMessage, HelloMessage, P2PMessageCode}
import org.lipicalabs.lipica.core.net.channel.Channel
import org.lipicalabs.lipica.core.net.shh.ShhMessageCode
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzMessageCode
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.InvalidCipherTextException

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:11
 * YANAGISAWA, Kentaro
 */
class MessageCodec extends ByteToMessageCodec[Message] {
	import MessageCodec._

	private var _frameCodec: FrameCodec = null
	def frameCodec: FrameCodec = this._frameCodec

	private var _myKey = SystemProperties.CONFIG.myKey
	def myKey: ECKey = this._myKey

	private var _nodeId: ImmutableBytes = null
	def nodeId: ImmutableBytes = this._nodeId

	private var _remoteId: ImmutableBytes = null
	def remoteId: ImmutableBytes = this._remoteId

	private var _handshake: EncryptionHandshake = null
	def handshake: EncryptionHandshake = this._handshake

	private var _initiatePacket: Array[Byte] = null
	def initiatePacket: Array[Byte] = this._initiatePacket

	private var _channel: Channel = null
	def channel: Channel = this._channel

	private var _messageCodesResolver: MessageCodesResolver = null
	def initMessageCodes(capabilities: Seq[Capability]): Unit = {
		this._messageCodesResolver = new MessageCodesResolver(capabilities)
	}

	private var _isHandshakeDone: Boolean = false
	def isHandshakeDone: Boolean = this._isHandshakeDone

	private def worldManager: WorldManager = WorldManager.instance

	private var _p2pMessageFactory: MessageFactory = null
	def setP2PMessageFactory(v: MessageFactory): Unit = this._p2pMessageFactory = v

	private var _lpcMessageFactory: MessageFactory = null
	def setLpcMessageFactory(v: MessageFactory): Unit = this._lpcMessageFactory = v

	private var _shhMessageFactory: MessageFactory = null
	def setShhMessageFactory(v: MessageFactory): Unit = this._shhMessageFactory = v

	private var _bzzMessageFactory: MessageFactory = null
	def setBzzMessageFactory(v: MessageFactory): Unit = this._bzzMessageFactory = v

	val initiator = new InitiateHandler

	class InitiateHandler extends ChannelInboundHandlerAdapter {
		override def channelActive(ctx: ChannelHandlerContext): Unit = {
			channel.inetSocketAddress = ctx.channel.remoteAddress.asInstanceOf[InetSocketAddress]
			if (remoteId.length == 64) {
				channel.setNode(remoteId)
				initiate(ctx)
			} else {
				_handshake = EncryptionHandshake.createResponder
				_nodeId = ImmutableBytes(myKey.getNodeId)
			}
		}
	}

	def initiate(ctx: ChannelHandlerContext): Unit = {
		loggerNet.info("<MessageCodec> Transport activated.")

		this._nodeId = ImmutableBytes(myKey.getNodeId)
		val remotePublicBytes = new Array[Byte](remoteId.length + 1)
		remoteId.copyTo(0, remotePublicBytes, 1, remoteId.length)
		remotePublicBytes(0) = 0x04//uncomporessed.
		val remotePublicKeyPoint = ECKey.fromPublicOnly(remotePublicBytes).getPubKeyPoint
		this._handshake = EncryptionHandshake.createInitiator(remotePublicKeyPoint)
		val initiateMessage = handshake.createAuthInitiate(myKey)
		this._initiatePacket = handshake.encryptAuthInitiate(initiateMessage)

		val byteBuff = ctx.alloc.buffer(initiatePacket.length)
		byteBuff.writeBytes(this.initiatePacket)
		ctx.writeAndFlush(byteBuff).sync()

		channel.nodeStatistics.transportAuthMessageSent.add

		loggerNet.info("<MessageCodec> To: %s, Sent: %s".format(ctx.channel.remoteAddress, initiateMessage))
	}

	override def encode(ctx: ChannelHandlerContext, message: Message, out: ByteBuf): Unit = {
		val output = "To: %s, Sending: %s".format(ctx.channel.remoteAddress, message)
		worldManager.listener.trace(output)
		loggerNet.info(output)

		val encoded = message.toEncodedBytes
		if (loggerWire.isDebugEnabled) {
			loggerWire.debug("<MessageCodec> Encoded: %s [%s]".format(message.command, encoded))
		}
		val code  = getCode(message.command)
		val frame = new Frame(code, encoded.toByteArray)
		this._frameCodec.writeFrame(frame, out)
		this._channel.nodeStatistics.transportOutMessages.add
	}

	override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
		if (loggerWire.isDebugEnabled) {
			loggerWire.debug("<MessageCodec> Received packet bytes: %,d".format(in.readableBytes))
		}
		if (!this._isHandshakeDone) {
			if (loggerWire.isDebugEnabled) {
				loggerWire.debug("<MessageCodec> Decoding handshake.")
			}
			decodeHandshake(ctx, in)
		} else {
			decodeMessage(ctx, in, out)
		}
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		val output = "<MessageCode> Exception caught: %s".format(cause.getClass.getSimpleName)
		if (this._channel.isDiscoveryMode) {
			loggerNet.debug(output, cause)
		} else {
			loggerNet.warn(output, cause)
		}
		ctx.close()
	}

	private def decodeHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf): Unit = {
		if (this._handshake.isInitiator) {
			if (this._frameCodec eq null) {
				val responsePacket = new Array[Byte](AuthResponseMessage.length + ECIESCoder.getOverhead)
				if (!buffer.isReadable(responsePacket.length)) {
					return
				}
				buffer.readBytes(responsePacket)
				val response = this._handshake.handleAuthResponse(myKey, this._initiatePacket, responsePacket)
				loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, response))
				val secrets = this._handshake.secrets
				this._frameCodec = new FrameCodec(secrets)
				loggerNet.info("<MessageCodec> Key exchange is done.")
				this._channel.sendHelloMessage(ctx, this._frameCodec, this._nodeId.toHexString)
			} else {
				val frame = this._frameCodec.readFrame(buffer)
				if (frame eq null) {
					return
				}
				val payload = ImmutableBytes(ByteStreams.toByteArray(frame.payload))
				if (frame.frameType == P2PMessageCode.Hello.asByte) {
					val helloMessage = HelloMessage.decode(payload)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, helloMessage))
					this._isHandshakeDone = true
					this._channel.publicTransportHandshakeFinished(ctx, helloMessage)
				} else {
					val disconnect = DisconnectMessage.decode(payload)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, disconnect))
					this._channel.nodeStatistics.nodeDisconnectedRemote(disconnect.reason)
				}
			}
		} else {
			if (loggerWire.isDebugEnabled) {
				loggerWire.debug("<MessageCode> Not initiator.")
			}
			if (this._frameCodec eq null) {
				val authInitPacket = new Array[Byte](AuthInitiateMessage.length + ECIESCoder.getOverhead)
				if (!buffer.isReadable(authInitPacket.length)) {
					return
				}
				buffer.readBytes(authInitPacket)
				this._handshake = EncryptionHandshake.createResponder
				try {
					val initiateMessage = this._handshake.decryptAuthInitiate(authInitPacket, myKey)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, initiateMessage))
					val response: AuthResponseMessage = this._handshake.makeResponse(initiateMessage, myKey)
					loggerNet.info("<MessageCodec> To: %s, Sending: %s".format(ctx.channel.remoteAddress, response))
					val responsePacket = this._handshake.encryptAuthResponse(response)
					this._handshake.agreeSecret(authInitPacket, responsePacket)

					val secrets = this._handshake.secrets
					this._frameCodec = new FrameCodec(secrets)

					val remotePublicKey = this._handshake.remotePublicKey
					val compressed = remotePublicKey.getEncoded()

					val remoteIdBytes = new Array[Byte](compressed.length - 1)
					System.arraycopy(compressed, 1, remoteIdBytes, 0, remoteIdBytes.length)
					this._remoteId = ImmutableBytes(remoteIdBytes)
					this._channel.setNode(this._remoteId)

					val byteBuffer = ctx.alloc.buffer(responsePacket.length)
					byteBuffer.writeBytes(responsePacket)
					ctx.writeAndFlush(byteBuffer).sync()
				} catch {
					case e: InvalidCipherTextException =>
						loggerNet.warn("<MessageCodec> Failed to decrypt the auth initiate message.")
				}
			} else {
				val frame = this._frameCodec.readFrame(buffer)
				if (frame eq null) {
					return
				}
				(new P2PMessageFactory).create(frame.frameType.toByte, ImmutableBytes(ByteStreams.toByteArray(frame.payload))) match {
					case Some(message) =>
						loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, message))
						if (frame.frameType.toByte == P2PMessageCode.Disconnect.asByte) {
							loggerNet.info("<MessageCodec> Active remote peer disconnected right after the handshake.")
							return
						}
						if (frame.frameType.toByte != P2PMessageCode.Hello.asByte) {
							loggerNet.info("<MessageCodec> Invalid message: %s".format(message))
							return
						}
						this._isHandshakeDone = true
						this._channel.sendHelloMessage(ctx, this._frameCodec, this.nodeId.toHexString)
						this._channel.publicTransportHandshakeFinished(ctx, message.asInstanceOf[HelloMessage])
					case None => ()
				}
			}
		}
		this._channel.nodeStatistics.transportInHello.add
	}

	private def decodeMessage(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
		if (in.readableBytes == 0) {
			return
		}
		Option(this._frameCodec.readFrame(in)) match {
			case Some(frame) =>
				val payload = ByteStreams.toByteArray(frame.payload)
				if (loggerWire.isDebugEnabled) {
					loggerWire.debug("<MessageCodec> Received: %s [%,d bytes]".format(frame.frameType, payload.length))
				}
				val messageOption = createMessage(frame.frameType.toByte, ImmutableBytes(payload))
				messageOption.foreach {
					message => {
						loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, message))
						worldManager.listener.onReceiveMessage(message)

						out.add(message)
						this._channel.nodeStatistics.transportInMessages.add
					}
				}
			case None => ()
		}
	}

	private def createMessage(code: Byte, payload: ImmutableBytes): Option[Message] = {
		MessageCodec.createMessage(
			this._messageCodesResolver.resolveP2P(code),
			this._p2pMessageFactory,
			(c: Byte) => P2PMessageCode.inRange(c),
			payload
		).foreach(m => return Option(m))

		MessageCodec.createMessage(
			this._messageCodesResolver.resolveLpc(code),
			this._lpcMessageFactory,
			(c: Byte) => LpcMessageCode.inRange(c),
			payload
		).foreach(m => return Option(m))

		MessageCodec.createMessage(
			this._messageCodesResolver.resolveShh(code),
			this._shhMessageFactory,
			(c: Byte) => ShhMessageCode.inRange(c),
			payload
		).foreach(m => return Option(m))

		MessageCodec.createMessage(
			this._messageCodesResolver.resolveBzz(code),
			this._bzzMessageFactory,
			(c: Byte) => BzzMessageCode.inRange(c),
			payload
		).foreach(m => return Option(m))

		None
	}

	private def getCode(command: Command): Byte = {
		command match {
			case c: P2PMessageCode => this._messageCodesResolver.withP2POffset(c.asByte)
			case c: LpcMessageCode => this._messageCodesResolver.withLpcOffset(c.asByte)
			case c: ShhMessageCode => this._messageCodesResolver.withShhOffset(c.asByte)
			case c: BzzMessageCode => this._messageCodesResolver.withBzzOffset(c.asByte)
			case _ => 0
		}
	}

	def setRemoteId(remoteId: String, channel: Channel): Unit = {
		this._remoteId = ImmutableBytes.parseHexString(remoteId)
		this._channel = channel
	}

	def generateTempKey(): Unit = {
		this._myKey = new ECKey().decompress
	}
}

object MessageCodec {
	private val loggerWire = LoggerFactory.getLogger("wire")
	private val loggerNet = LoggerFactory.getLogger("net")

	private def createMessage(resolvedCode: Byte, factory: MessageFactory, predicate: (Byte) => Boolean, payload: ImmutableBytes): Option[Message] = {
		if ((factory ne null) && predicate(resolvedCode)) {
			factory.create(resolvedCode, payload)
		} else {
			None
		}
	}

}
