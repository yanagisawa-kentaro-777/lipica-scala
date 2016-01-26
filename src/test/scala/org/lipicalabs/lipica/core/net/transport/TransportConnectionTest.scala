package org.lipicalabs.lipica.core.net.transport

import java.io.{DataInputStream, ByteArrayInputStream, PipedOutputStream, PipedInputStream}
import java.security.SecureRandom

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
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

	private var to: PipedInputStream = null
	private var toOut: PipedOutputStream = null
	private var from: PipedInputStream = null
	private var fromOut: PipedOutputStream = null

	private def init(): Unit = {
		val remoteKey = ECKeyPair(new SecureRandom()).decompress
		val myKey = ECKeyPair(new SecureRandom()).decompress

		this.initiator = EncryptionHandshake.createInitiator(remoteKey.publicKeyPoint)
		this.responder = EncryptionHandshake.createResponder

		val initiate = this.initiator.createAuthInitiate(myKey)
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

}
