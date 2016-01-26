package org.lipicalabs.lipica.core.net.transport

import java.net.InetSocketAddress
import java.security.SecureRandom
import java.util

import com.google.common.io.ByteStreams
import io.netty.buffer.ByteBuf
import io.netty.channel.{ChannelInboundHandlerAdapter, ChannelHandlerContext}
import io.netty.handler.codec.ByteToMessageCodec
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.crypto.ECIESCoder
import org.lipicalabs.lipica.core.crypto.elliptic_curve.{ECPublicKey, ECKeyPair}
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.{Command, MessageFactory, Message}
import org.lipicalabs.lipica.core.net.p2p.{P2PMessageFactory, DisconnectMessage, HelloMessage, P2PMessageCode}
import org.lipicalabs.lipica.core.net.channel.Channel
import org.lipicalabs.lipica.core.net.peer_discovery.NodeId
import org.lipicalabs.lipica.core.net.shh.ShhMessageCode
import org.lipicalabs.lipica.core.net.swarm.bzz.BzzMessageCode
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.lipicalabs.lipica.core.utils.{ErrorLogger, ImmutableBytes}
import org.slf4j.LoggerFactory
import org.spongycastle.crypto.InvalidCipherTextException

/**
 * バイト配列と、このプログラムにとって意味を持つメッセージとの相互変換を実行するクラスです。
 * Nettyのインターフェイスに準拠しています。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:11
 * YANAGISAWA, Kentaro
 */
class MessageCodec extends ByteToMessageCodec[Message] {
	import MessageCodec._

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private var _frameCodec: FrameCodec = null
	def frameCodec: FrameCodec = this._frameCodec

	private var _myKey = NodeProperties.CONFIG.privateKey
	def myKey: ECKeyPair = this._myKey

	private var _nodeId: NodeId = null
	def nodeId: NodeId = this._nodeId

	private var _remoteNodeId: NodeId = null
	def remoteNodeId: NodeId = this._remoteNodeId

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
			if (remoteNodeId.length == 64) {
				//相手を特定したセッション開始者側（＝クライアント側）。
				channel.assignNode(remoteNodeId)
				initiate(ctx)
			} else {
				//受動的な応答者側（＝サーバー側）。
				_handshake = EncryptionHandshake.createResponder
				_nodeId = myKey.toNodeId
			}
		}
	}

	/**
	 * クライアントとして、新たなセッションを開始しようとします。
	 */
	private def initiate(ctx: ChannelHandlerContext): Unit = {
		loggerNet.info("<MessageCodec> Transport activated.")

		this._nodeId = myKey.toNodeId
		//相手ノードの公開鍵を構築する。
		val remotePublicBytes = new Array[Byte](remoteNodeId.length + 1)
		remoteNodeId.bytes.copyTo(0, remotePublicBytes, 1, remoteNodeId.length)
		remotePublicBytes(0) = 0x04//uncomporessed.
		val remotePublicKeyPoint = ECPublicKey.fromPublicOnly(remotePublicBytes).publicKeyPoint

		//相手ノードに送信するセッション確立要求メッセージを生成する。
		this._handshake = EncryptionHandshake.createInitiator(remotePublicKeyPoint)
		val initiateMessage = handshake.createAuthInitiate(myKey)
		this._initiatePacket = handshake.encryptAuthInitiate(initiateMessage)

		//セッション確立要求メッセージをネットワークに送信する。
		val byteBuff = ctx.alloc.buffer(initiatePacket.length)
		byteBuff.writeBytes(this.initiatePacket)
		ctx.writeAndFlush(byteBuff).sync()
		//動作統計情報を更新する。
		channel.nodeStatistics.transportAuthMessageSent.add

		loggerNet.info("<MessageCodec> To: %s, Sent: %s".format(ctx.channel.remoteAddress, initiateMessage))
	}

	/**
	 * 渡されたメッセージをネットワークに送信します。
	 */
	override def encode(ctx: ChannelHandlerContext, message: Message, out: ByteBuf): Unit = {
		val output = "To: %s, Sending: %s".format(ctx.channel.remoteAddress, message)
		componentsMotherboard.listener.trace(output)
		loggerNet.info(output)

		val encoded = message.toEncodedBytes
		if (loggerWire.isDebugEnabled) {
			loggerWire.debug("<MessageCodec> Encoded: %s [%,d bytes]".format(message.command, encoded.length))
		}
		val code  = getCode(message.command)
		val frame = new Frame(code, encoded.toByteArray)
		//暗号化やMACの処理は、FrameCodec に移譲する。
		this.frameCodec.writeFrame(frame, out)
		//動作統計情報を更新する。
		this.channel.nodeStatistics.transportOutMessages.add
	}

	override def decode(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
		//TCPはストリームであって、メッセージ解析の失敗は尾を引くので、
		//このメソッドでは例外を捕捉しない。
		if (loggerWire.isDebugEnabled) {
			loggerWire.debug("<MessageCodec> Received packet bytes: %,d".format(in.readableBytes))
		}
		if (!this.isHandshakeDone) {
			if (loggerWire.isDebugEnabled) {
				loggerWire.debug("<MessageCodec> Decoding handshake.")
			}
			//まだ秘密情報を共有できていないので、FrameCodec を経由せずにハンドシェイクを実行する。
			decodeHandshake(ctx, in)
		} else {
			//まだ秘密情報を共有できているので、FrameCodec を経由してメッセージの再構築を行う。
			decodeMessage(ctx, in, out)
		}
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		val output = "<MessageCodec> Exception caught: %s".format(cause.getClass.getSimpleName)
		if (this._channel.isDiscoveryMode) {
			loggerNet.debug(output, cause)
		} else {
			ErrorLogger.logger.warn(output, cause)
			loggerNet.warn(output, cause)
		}
		//TCPはストリームなので、１個のメッセージの解析に失敗したら、もうその受信ハンドラは見捨てざるを得ない。
		ctx.close()
	}

	/**
	 * 渡されたメッセージに基いて、ハンドシェイクを実行します。
	 * これが完了すれば、相手ノードとの間に秘密を共有できたことになります。
	 */
	private def decodeHandshake(ctx: ChannelHandlerContext, buffer: ByteBuf): Unit = {
		if (this.handshake.isInitiator) {
			//こちらがクライアントである。
			if (this.frameCodec eq null) {
				//まだ FrameCodec も生成されていない、ということは、
				//今回受け取ったのは、セッション開始要求に対する応答のはず。
				val responsePacket = new Array[Byte](AuthResponseMessage.length + ECIESCoder.getOverhead)
				if (!buffer.isReadable(responsePacket.length)) {
					return
				}
				buffer.readBytes(responsePacket)
				//応答を解釈し、秘密情報について合意する。
				val response = this.handshake.handleAuthResponse(myKey, this._initiatePacket, responsePacket)
				loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, response))
				val secrets = this.handshake.secrets
				//合意された秘密情報を利用する FrameCodec を生成する。
				this._frameCodec = new FrameCodec(secrets)
				loggerNet.info("<MessageCodec> Key exchange is done.")
				this.channel.sendHelloMessage(ctx, this.frameCodec, this.nodeId)
			} else {
				//FrameCodec は生成されている。今回受け取ったのは、HelloMessage もしくは DisconnectMessage のはずである。
				val frame = this.frameCodec.readFrame(buffer)
				if (frame eq null) {
					return
				}
				val payload = ImmutableBytes(ByteStreams.toByteArray(frame.payload))
				if (frame.frameType == P2PMessageCode.Hello.asByte) {
					val helloMessage = HelloMessage.decode(payload)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, helloMessage))
					//ハンドシェイクが完了した。
					this._isHandshakeDone = true
					this.channel.publicTransportHandshakeFinished(ctx, helloMessage)
				} else {
					//さようなら。
					val disconnect = DisconnectMessage.decode(payload)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, disconnect))
					this.channel.nodeStatistics.nodeDisconnectedRemote(disconnect.reason)
				}
			}
		} else {
			//こちらがサーバー側である。
			if (loggerWire.isDebugEnabled) {
				loggerWire.debug("<MessageCodec> Responder.")
			}
			if (this._frameCodec eq null) {
				//まだ FrameCodec も生成されていない、ということは、
				//今回受け取ったのは、セッション開始要求であるはず。
				val authInitPacket = new Array[Byte](AuthInitiateMessage.length + ECIESCoder.getOverhead)
				if (!buffer.isReadable(authInitPacket.length)) {
					return
				}
				buffer.readBytes(authInitPacket)
				this._handshake = EncryptionHandshake.createResponder
				try {
					val initiateMessage = this.handshake.decryptAuthInitiate(authInitPacket, myKey)
					loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, initiateMessage))
					//受け取った要求メッセージに対する応答メッセージを生成する。
					val response: AuthResponseMessage = this.handshake.makeResponse(initiateMessage, myKey)
					loggerNet.info("<MessageCodec> To: %s, Sending: %s".format(ctx.channel.remoteAddress, response))
					val responsePacket = this.handshake.encryptAuthResponse(response)
					//要求および応答のペアから、秘密情報を共有する。
					this.handshake.agreeSecrets(authInitPacket, responsePacket)
					val secrets = this.handshake.secrets
					//共有された秘密情報に基いて、FrameCodec を生成する。
					this._frameCodec = new FrameCodec(secrets)

					val remotePublicKey = this.handshake.remotePublicKey
					val compressed = remotePublicKey.getEncoded(true)

					//判明した相手ノードの公開鍵から、相手ノードのノードIDを生成して記憶する。
					val remoteIdBytes = new Array[Byte](compressed.length - 1)
					System.arraycopy(compressed, 1, remoteIdBytes, 0, remoteIdBytes.length)
					this._remoteNodeId = NodeId(remoteIdBytes)
					this.channel.assignNode(this.remoteNodeId)

					//応答をネットワークに送信する。
					val byteBuffer = ctx.alloc.buffer(responsePacket.length)
					byteBuffer.writeBytes(responsePacket)
					ctx.writeAndFlush(byteBuffer).sync()
				} catch {
					case e: InvalidCipherTextException =>
						ErrorLogger.logger.warn("<MessageCodec> Failed to decrypt the auth initiate message.")
						loggerNet.warn("<MessageCodec> Failed to decrypt the auth initiate message.")
				}
			} else {
				//FrameCodec は生成されている。今回受け取ったのは、HelloMessage もしくは DisconnectMessage のはずである。
				val frame = this.frameCodec.readFrame(buffer)
				if (frame eq null) {
					return
				}
				(new P2PMessageFactory).create(frame.frameType.toByte, ImmutableBytes(ByteStreams.toByteArray(frame.payload))) match {
					case Some(message) =>
						loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, message))
						if (frame.frameType.toByte == P2PMessageCode.Disconnect.asByte) {
							//さようなら。
							loggerNet.info("<MessageCodec> Active remote peer disconnected right after the handshake.")
							return
						}
						if (frame.frameType.toByte != P2PMessageCode.Hello.asByte) {
							//不明なメッセージ。
							loggerNet.info("<MessageCodec> Invalid message: %s".format(message))
							return
						}
						//HelloMessageを受信した。
						//ハンドシェイクは完了とする。
						this._isHandshakeDone = true
						this.channel.sendHelloMessage(ctx, this.frameCodec, this.nodeId)
						this.channel.publicTransportHandshakeFinished(ctx, message.asInstanceOf[HelloMessage])
					case None =>
						//
				}
			}
		}
		//動作統計情報を更新する。
		this.channel.nodeStatistics.transportInHello.add
	}

	/**
	 * 渡されたバイト配列からメッセージを再構築し、nettyのパイプラインに流します。
	 */
	private def decodeMessage(ctx: ChannelHandlerContext, in: ByteBuf, out: util.List[AnyRef]): Unit = {
		if (in.readableBytes == 0) {
			return
		}
		Option(this.frameCodec.readFrame(in)) match {
			case Some(frame) =>
				//フレームの読み取りに成功した。
				val payload = ByteStreams.toByteArray(frame.payload)
				if (loggerWire.isDebugEnabled) {
					loggerWire.debug("<MessageCodec> Received: %s [%,d bytes]".format(frame.frameType, payload.length))
				}
				//メッセージを生成する。
				val messageOption = createMessage(frame.frameType.toByte, ImmutableBytes(payload))
				messageOption.foreach {
					message => {
						loggerNet.info("<MessageCodec> From: %s, Received: %s".format(ctx.channel.remoteAddress, message))
						//イベントをファイアする。
						componentsMotherboard.listener.onReceiveMessage(message)
						//nettyのパイプラインに流す。
						out.add(message)
						//稼働統計情報を更新する。
						this.channel.nodeStatistics.transportInMessages.add
					}
				}
			case None => ()
		}
	}

	/**
	 * 渡されたメッセージ種別とペイロードから、
	 * メッセージオブジェクトを再構築して返します。
	 */
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

	def assignRemoteNodeId(aRemoteNodeId: NodeId, channel: Channel): Unit = {
		this._remoteNodeId = aRemoteNodeId
		this._channel = channel
	}

	def generateTemporaryKey(): Unit = {
		this._myKey = ECKeyPair(new SecureRandom).decompress
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
