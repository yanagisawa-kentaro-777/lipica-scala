package org.lipicalabs.lipica.core.net.transport

import java.security.SignatureException
import java.util

import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.net.peer_discovery.message.{FindNodeMessage, NeighborsMessage, PongMessage, PingMessage}
import org.lipicalabs.lipica.core.utils.{ByteUtils, ErrorLogger, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/15 13:19
 * YANAGISAWA, Kentaro
 */
class TransportMessage {

	protected val logger = TransportMessage.logger

	private var _wire: Array[Byte] = null
	def packet: Array[Byte] = this._wire

	private var _mdc: ImmutableBytes = null
	def mdc: ImmutableBytes = this._mdc

	private var _signature: ImmutableBytes = null
	def signature: ImmutableBytes = this._signature

	private var _messageType: ImmutableBytes = null
	def messageType: ImmutableBytes = this._messageType

	private var _data: ImmutableBytes = null
	def data: ImmutableBytes = this._data

	def key: Either[Throwable, ECKey] = {
		try {
			val r = this.signature.copyOfRange(0, 32)
			val s = this.signature.copyOfRange(32, 64)
			val v: Byte = this.signature(64) match {
				case 0 => 27
				case 1 => 28
				case b => b
			}
			val generatedSignature = ECKey.ECDSASignature.fromComponents(r.toByteArray, s.toByteArray, v)
			val messageHash = DigestUtils.digest256(util.Arrays.copyOfRange(this._wire, 97, this._wire.length))

			//署名から公開鍵を生成して返す。
			Right(ECKey.signatureToKey(messageHash, generatedSignature.toBase64))
		} catch {
			case e: SignatureException =>
				ErrorLogger.logger.warn("<TransportMessage> Signature exception.", e)
				logger.warn("<TransportMessage> Signature exception.", e)
				Left(e)
			case any: Throwable =>
				ErrorLogger.logger.warn("<TransportMessage> Exception caught: %s".format(any.getClass.getSimpleName), any)
				logger.warn("<TransportMessage> Exception caught: %s".format(any.getClass.getSimpleName), any)
				Left(any)
		}
	}

	def nodeId: ImmutableBytes = {
		this.key match {
			case Right(k) =>
				val publicKey = ImmutableBytes(k.getPubKey)
				publicKey.copyOfRange(1, 65)
			case Left(e) =>
				ImmutableBytes.empty
		}
	}

	protected def parse(wire: Array[Byte]): Unit = {
		//override用。
	}

	override def toString: String = {
		"{mdc=%s, signature=%s, type=%s, data=%s}".format(this.mdc, this.signature, this.messageType, this.data)
	}

}

object TransportMessage {
	private val logger = LoggerFactory.getLogger("net")

	def encode[T <: TransportMessage](messageType: Array[Byte], data: ImmutableBytes, privateKey: ECKey): T = {
		if (logger.isDebugEnabled) {
			logger.debug("<TransportMessage> Encoding: %d".format(messageType.head))
		}
		//ダイジェスト値を計算する。
		val payload = new Array[Byte](messageType.length + data.length)
		payload(0) = messageType(0)
		data.copyTo(0, payload, 1, data.length)
		val forSig = DigestUtils.digest256(payload)

		//署名を生成する。
		val signature = privateKey.sign(forSig)
		signature.v = (signature.v - 27).toByte

		val rPart = ByteUtils.bigIntegerToBytes(signature.r, 32)
		val sPart = ByteUtils.bigIntegerToBytes(signature.s, 32)
		val signatureBytes = rPart ++ sPart ++ Array[Byte](signature.v)

		//MDCを計算する。
		val dataBytes = data.toByteArray
		val digest = signatureBytes ++ messageType ++ dataBytes
		val mdc = DigestUtils.digest256(digest)

		//結果インスタンスを生成する。
		val result = createMessage(messageType(0))
		result._mdc = ImmutableBytes(mdc)
		result._signature = ImmutableBytes(signatureBytes)
		result._messageType = ImmutableBytes(messageType)
		result._data = data
		result._wire = mdc ++ signatureBytes ++ messageType ++ dataBytes

		result.asInstanceOf[T]
	}

	def decode[T <: TransportMessage](wire: Array[Byte]): Either[Throwable, T] = {
		try {
			if (wire.length < 98) {
				throw new IllegalArgumentException("Message too short %,d < %,d".format(wire.length, 98))
			}
			val mdc = util.Arrays.copyOfRange(wire, 0, 32)
			val signature = util.Arrays.copyOfRange(wire, 32, 32 + 65)
			val messageType = Array[Byte](wire(97))

			if (logger.isDebugEnabled) {
				logger.debug("<TransportMessage> Decoding: %d".format(messageType.head))
			}

			val data = util.Arrays.copyOfRange(wire, 98, wire.length)

			val mdcCheck = DigestUtils.digest256(util.Arrays.copyOfRange(wire, 32, wire.length))
			if (!util.Arrays.equals(mdc, mdcCheck)) {
				throw new IllegalArgumentException("MDC check failed.")
			}

			val result = createMessage(messageType(0))
			result._mdc = ImmutableBytes(mdc)
			result._signature = ImmutableBytes(signature)
			result._messageType = ImmutableBytes(messageType)
			result._data = ImmutableBytes(data)
			result._wire = wire
			result.parse(data)
			Right(result.asInstanceOf[T])
		} catch {
			case any: Throwable =>
				Left(any)
		}
	}

	private def createMessage(messageType: Byte): TransportMessage = {
		messageType match {
			case 1 => new PingMessage
			case 2 => new PongMessage
			case 3 => new FindNodeMessage
			case 4 => new NeighborsMessage
			case _ => throw new IllegalArgumentException("Unknown message type: %d".format(messageType))
		}
	}

}
