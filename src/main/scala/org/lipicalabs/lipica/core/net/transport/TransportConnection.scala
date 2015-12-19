package org.lipicalabs.lipica.core.net.transport

import java.io.{IOException, DataInputStream, OutputStream, InputStream}

import org.lipicalabs.lipica.core.net.p2p.P2PMessage
import org.lipicalabs.lipica.core.net.transport.EncryptionHandshake.Secrets
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 12:40
 * YANAGISAWA, Kentaro
 */
class TransportConnection(private val secrets: Secrets, inputStream: InputStream, private val out: OutputStream) {
	import TransportConnection._

	private val codec = new FrameCodec(this.secrets)
	private val in: DataInputStream = new DataInputStream(inputStream)

	private var _handshakeMessage: HandshakeMessage = null
	def getHandshakeMessage: HandshakeMessage = this._handshakeMessage

	def sendProtocolHandshake(message: HandshakeMessage): Unit = {
		val payload = message.encode
		this.codec.writeFrame(new Frame(HandshakeMessage.HandshakeMessageType, payload), this.out)
	}

	def writeMessage(message: P2PMessage): Unit = {
		val payload = message.toEncodedBytes
		this.codec.writeFrame(new Frame(message.command.asByte, payload.toByteArray), out)
	}

	def handleNextMessage(): Unit = {
		val frame = this.codec.readFrame(this.in)
		if (this._handshakeMessage eq null) {
			if (frame.frameType.toInt != HandshakeMessage.HandshakeMessageType) {
				throw new IOException("Illegal message.")
			}
			val wire = new Array[Byte](frame.size)
			frame.payload.read(wire)
			this._handshakeMessage = HandshakeMessage.decode(wire)
			logger.info("<TransportConnection> " + this.getHandshakeMessage)
		} else {
			val wire = new Array[Byte](frame.size)
			frame.payload.read(wire)
		}
	}

}

object TransportConnection {
	private val logger = LoggerFactory.getLogger("discover")
}