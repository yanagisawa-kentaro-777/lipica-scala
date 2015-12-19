package org.lipicalabs.lipica.core.net.transport

import java.io._

import io.netty.buffer.{ByteBufInputStream, ByteBufOutputStream, ByteBuf}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.RBACCodec
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.engines.AESFastEngine
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.{KeyParameter, ParametersWithIV}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 11:04
 * YANAGISAWA, Kentaro
 */
class FrameCodec(secrets: EncryptionHandshake.Secrets) {

	import FrameCodec._

	private val blockBits = secrets.aes.length * 8
	private val enc = new SICBlockCipher(new AESFastEngine)
	this.enc.init(true, new ParametersWithIV(new KeyParameter(secrets.aes.toByteArray), new Array[Byte](blockBits / 8)))

	private val dec = new SICBlockCipher(new AESFastEngine)
	this.dec.init(false, new ParametersWithIV(new KeyParameter(secrets.aes.toByteArray), new Array[Byte](blockBits / 8)))

	private val egressMac = secrets.egressMac
	private val ingressMac = secrets.ingressMac

	private val mac = secrets.mac

	private var _isHeadRead: Boolean = false
	def isHeadRead: Boolean = this._isHeadRead

	private var _totalBodySize: Int = 0
	def totalBodySize: Int = this._totalBodySize

	private def makeMacCipher: AESFastEngine = {
		val macc = new AESFastEngine
		macc.init(true, new KeyParameter(this.mac.toByteArray))
		macc
	}

	def writeFrame(frame: Frame, buf: ByteBuf): Unit = {
		writeFrame(frame, new ByteBufOutputStream(buf))
	}

	def writeFrame(frame: Frame, out: OutputStream): Unit = {
		val headBuffer = new Array[Byte](32)
		val ptype = RBACCodec.Encoder.encode(frame.frameType.toInt).toByteArray
		val totalSize: Int = frame.size + ptype.length
		headBuffer(0) = (totalSize >> 16).toByte
		headBuffer(1) = (totalSize >> 8).toByte
		headBuffer(2) = totalSize.toByte
		val headerData = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(RBACCodec.Encoder.encode(0)))
		headerData.copyTo(0, headBuffer, 3, headerData.length)

		this.enc.processBytes(headBuffer, 0, 16, headBuffer, 0)

		// Header MAC
		updateMac(egressMac, headBuffer, 0, headBuffer, 16, egress = true)

		val buff = new Array[Byte](256)
		out.write(headBuffer)
		enc.processBytes(ptype, 0, ptype.length, buff, 0)
		out.write(buff, 0, ptype.length)
		egressMac.update(buff, 0, ptype.length)
		var shouldContinue = true
		while (shouldContinue) {
			val n = frame.payload.read(buff)
			if (0 < n) {
				enc.processBytes(buff, 0, n, buff, 0)
				egressMac.update(buff, 0, n)
				out.write(buff, 0, n)
			} else {
				shouldContinue = false
			}
		}
		val padding = 16 - (totalSize % 16)
		val pad = new Array[Byte](16)
		if (padding < 16) {
			enc.processBytes(pad, 0, padding, buff, 0)
			egressMac.update(buff, 0, padding)
			out.write(buff, 0, padding)
		}

		// Frame MAC
		val macBuffer = new Array[Byte](egressMac.getDigestSize)
		DigestUtils.doSum(egressMac, macBuffer)
		updateMac(egressMac, macBuffer, 0, macBuffer, 0, egress = true)
		out.write(macBuffer, 0, 16)
	}

	def readFrame(buf: ByteBuf): Frame = {
		readFrame(new ByteBufInputStream(buf))
	}

	def readFrame(inp: DataInput): Frame = {
		if (!isHeadRead) {
			val headBuffer = new Array[Byte](32)
			try {
				inp.readFully(headBuffer)
			} catch {
				case e: EOFException =>
					return null
			}
			updateMac(ingressMac, headBuffer, 0, headBuffer, 16, egress = false)
			dec.processBytes(headBuffer, 0, 16, headBuffer, 0)
			this._totalBodySize = headBuffer(0)
			this._totalBodySize = (this._totalBodySize << 8) + (headBuffer(1) & 0xFF)
			this._totalBodySize = (this._totalBodySize << 8) + (headBuffer(2) & 0xFF)
			this._isHeadRead = true
		}

		var padding = 16 - (totalBodySize % 16)
		if (padding == 16) {
			padding = 0
		}
		val macSize = 16
		val buffer = new Array[Byte](totalBodySize + padding + macSize)
		try {
			inp.readFully(buffer)
		} catch {
			case e: EOFException =>
				return null
		}
		val frameSize = buffer.length - macSize
		ingressMac.update(buffer, 0, frameSize)
		dec.processBytes(buffer, 0, frameSize, buffer, 0)
		var pos = 0
		val decoded = RBACCodec.Decoder.decode(buffer).right.get
		val frameType: Long = decoded.asPositiveLong
		pos = decoded.pos
		val payload = new ByteArrayInputStream(buffer, pos, totalBodySize - pos)
		val size = totalBodySize - pos
		val macBuffer = new Array[Byte](ingressMac.getDigestSize)

		// Frame MAC
		DigestUtils.doSum(ingressMac, macBuffer)
		updateMac(ingressMac, macBuffer, 0, buffer, frameSize, egress = false)

		this._isHeadRead = false
		new FrameCodec.Frame(frameType, size, payload)
	}

	private def updateMac(mac: KeccakDigest, seed: Array[Byte], offset: Int, out: Array[Byte], outOffset: Int, egress: Boolean): Array[Byte] = {
		val aesBlock = new Array[Byte](mac.getDigestSize)
		DigestUtils.doSum(mac, aesBlock)
		makeMacCipher.processBlock(aesBlock, 0, aesBlock, 0)
		// Note that although the mac digest size is 32 bytes, we only use 16 bytes in the computation
		val length = 16
		for (i <- 0 until length) {
			aesBlock(i) = (aesBlock(i) ^ seed(i + offset)).toByte
		}

		mac.update(aesBlock, 0, length)
		val result = new Array[Byte](mac.getDigestSize)
		DigestUtils.doSum(mac, result)
		if (egress) {
			for (i <- 0 until length) {
				out(i + outOffset) = result(i)
			}
		} else {
			for (i <- 0 until length) {
				if (out(i + outOffset) != result(i)) {
					throw new IOException("MAC mismatch")
				}
			}
		}
		result
	}

}

object FrameCodec {

	class Frame(val frameType: Long, val size: Int, val payload: InputStream) {
		def this(frameType: Int, payload: Array[Byte]) = {
			this(frameType, payload.length, new ByteArrayInputStream(payload))
		}
	}

}