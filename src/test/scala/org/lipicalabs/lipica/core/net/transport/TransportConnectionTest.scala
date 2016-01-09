package org.lipicalabs.lipica.core.net.transport

import java.io.{DataInputStream, ByteArrayInputStream, PipedOutputStream, PipedInputStream}
import java.security.SecureRandom

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.transport.FrameCodec.Frame
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class TransportConnectionTest extends Specification {
	sequential

	private var iCodec: FrameCodec = null
	private var rCodec: FrameCodec = null
	private var initiator: EncryptionHandshake = null
	private var responder: EncryptionHandshake = null
	private var iMessage: HandshakeMessage = null

	private var to: PipedInputStream = null
	private var toOut: PipedOutputStream = null
	private var from: PipedInputStream = null
	private var fromOut: PipedOutputStream = null

	private def init(): Unit = {
		val remoteKey = new ECKey().decompress
		val myKey = new ECKey().decompress

		this.initiator = EncryptionHandshake.createInitiator(remoteKey.getPubKeyPoint)
		this.responder = EncryptionHandshake.createResponder

		val initiate = this.initiator.createAuthInitiate(null, myKey)
		val initiatePacket = this.initiator.encryptAuthInitiate(initiate)
		val responsePacket = this.responder.handleAuthInitiate(initiatePacket, remoteKey)
		this.initiator.handleAuthResponse(myKey, initiatePacket, responsePacket)

		this.to = new PipedInputStream(1024 * 1024)
		this.toOut = new PipedOutputStream(this.to)
		this.from = new PipedInputStream(1024 * 1024)
		this.fromOut = new PipedOutputStream(this.from)

		this.iCodec = new FrameCodec(this.initiator.secrets)
		this.rCodec = new FrameCodec(this.responder.secrets)

		val nodeId = ImmutableBytes(Array[Byte](1, 2, 3, 4))
		this.iMessage = new HandshakeMessage(
			123,
			"abcd",
			Seq(new Capability("zz", 1), new Capability("yy", 3)),
			3333,
			nodeId
		)
	}

	"HandshakeMessage" should {
		"be right" in {
			init()

			val wire = this.iMessage.encode
			val rebuilt = HandshakeMessage.decode(wire)

			rebuilt.version mustEqual 123
			rebuilt.name mustEqual "abcd"
			rebuilt.listenPort mustEqual 3333
			rebuilt.nodeId mustEqual this.iMessage.nodeId
			rebuilt.capabilities mustEqual this.iMessage.capabilities
		}
	}

	"Frame" should {
		"be right" in {
			init()

			val originalPayload = new Array[Byte](123)
			new SecureRandom().nextBytes(originalPayload)
			val originalFrame: FrameCodec.Frame = new Frame(12345, 123, new ByteArrayInputStream(originalPayload))

			this.iCodec.writeFrame(originalFrame, toOut)
			val readFrame = this.rCodec.readFrame(new DataInputStream(this.to))

			val readPayload = new Array[Byte](readFrame.size)
			readFrame.payload.read(readPayload)

			originalFrame.frameType mustEqual readFrame.frameType
			originalFrame.size mustEqual readFrame.size
			readPayload mustEqual originalPayload
		}
	}

	"Handshake" should {
		"be right" in {
			init()

			val iConn = new TransportConnection(this.initiator.secrets, this.from, this.toOut)
			val rConn = new TransportConnection(this.responder.secrets, this.to, this.fromOut)

			iConn.sendProtocolHandshake(this.iMessage)
			rConn.handleNextMessage()

			val receivedMessage = rConn.getHandshakeMessage
			receivedMessage.version mustEqual 123
			receivedMessage.name mustEqual "abcd"
			receivedMessage.listenPort mustEqual 3333
			receivedMessage.nodeId mustEqual this.iMessage.nodeId
			receivedMessage.capabilities mustEqual this.iMessage.capabilities
		}
	}

}
