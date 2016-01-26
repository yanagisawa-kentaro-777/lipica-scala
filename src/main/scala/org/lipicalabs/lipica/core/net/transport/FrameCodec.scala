package org.lipicalabs.lipica.core.net.transport

import java.io._

import io.netty.buffer.{ByteBufInputStream, ByteBufOutputStream, ByteBuf}
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.spongycastle.crypto.digests.KeccakDigest
import org.spongycastle.crypto.engines.AESFastEngine
import org.spongycastle.crypto.modes.SICBlockCipher
import org.spongycastle.crypto.params.{KeyParameter, ParametersWithIV}

/**
 * ネットワーク（nettyのインターフェイス）に対して
 * バイト列のI/Oを実施するためのクラスです。
 *
 * 送信時には、バイト列から、ヘッダとボディを持つ「フレーム」を構築し、
 * その内容を暗号化し、改竄防止のための
 * MAC（message authentication code）を加えてネットワークに送信します。
 *
 * 受信時には、バイト列を復号して
 * そのMACが正しいことを検証した上で、平文のバイト列を復元します。
 *
 * @param secrets ECDHE（一時鍵を利用した楕円曲線DH鍵合意）によって合意された秘密情報。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 11:04
 * YANAGISAWA, Kentaro
 */
class FrameCodec(private val secrets: EncryptionHandshake.Secrets) {

	import FrameCodec._

	/**
	 * AES共通鍵のビット数。
	 */
	private val blockBits = secrets.aes.length * 8

	/**
	 * AESによる暗号化機構。
	 */
	private val encoder = new SICBlockCipher(new AESFastEngine)
	this.encoder.init(true, new ParametersWithIV(new KeyParameter(secrets.aes.toByteArray), new Array[Byte](blockBits / 8)))

	/**
	 * AESによる復号機構。
	 */
	private val decoder = new SICBlockCipher(new AESFastEngine)
	this.decoder.init(false, new ParametersWithIV(new KeyParameter(secrets.aes.toByteArray), new Array[Byte](blockBits / 8)))

	/**
	 * 送信されるデータによって更新されるダイジェスト計算器。
	 */
	private val egressMac: KeccakDigest = secrets.egressMac

	/**
	 * 受信したデータによって更新されるダイジェスト計算器。
	 */
	private val ingressMac: KeccakDigest = secrets.ingressMac

	private var _isHeadRead: Boolean = false
	def isHeadRead: Boolean = this._isHeadRead

	private var _totalBodySize: Int = 0
	def totalBodySize: Int = this._totalBodySize

	private def createMacCipherEngine: AESFastEngine = {
		val engine = new AESFastEngine
		engine.init(true, new KeyParameter(this.secrets.mac.toByteArray))
		engine
	}

	/**
	 * 渡されたフレーム（バイト列）を暗号化し、
	 * MAC（message authentication code）を加えてネットワークに書き込みます。
	 */
	def writeFrame(frame: Frame, buf: ByteBuf): Unit = {
		writeFrame(frame, new ByteBufOutputStream(buf))
	}

	/**
	 * 渡されたフレーム（バイト列）を暗号化し、
	 * MAC（message authentication code）を加えてネットワークに書き込みます。
	 */
	def writeFrame(frame: Frame, out: OutputStream): Unit = {
		val headBuffer = new Array[Byte](32)
		val encodedFrameType = RBACCodec.Encoder.encode(frame.frameType.toInt).toByteArray
		val totalSize: Int = frame.size + encodedFrameType.length
		headBuffer(0) = (totalSize >> 16).toByte
		headBuffer(1) = (totalSize >> 8).toByte
		headBuffer(2) = totalSize.toByte
		val headerData = RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(RBACCodec.Encoder.encode(0)))
		headerData.copyTo(0, headBuffer, 3, headerData.length)

		//前半16バイトを暗号化する。
		this.encoder.processBytes(headBuffer, 0, MacSize, headBuffer, 0)
		//後半16バイトに、ヘッダ分のMACを書き込む。
		updateMac(egressMac, headBuffer, 0, headBuffer, MacSize, egress = true)
		//ヘッダ部分を書き込む。
		out.write(headBuffer)

		//I/O単位を256バイトとする。
		val buff = new Array[Byte](256)

		//フレームタイプを暗号化して書き込む。
		encoder.processBytes(encodedFrameType, 0, encodedFrameType.length, buff, 0)
		out.write(buff, 0, encodedFrameType.length)
		//送信MACを更新する。
		egressMac.update(buff, 0, encodedFrameType.length)

		var shouldContinue = true
		while (shouldContinue) {
			val n = frame.payload.read(buff)
			if (0 < n) {
				//暗号化する。
				encoder.processBytes(buff, 0, n, buff, 0)
				//送信MACを更新する。
				egressMac.update(buff, 0, n)
				//ネットワークに書き込む。
				out.write(buff, 0, n)
			} else {
				//終端と見る。
				shouldContinue = false
			}
		}
		//ブロック単位の残りを、paddingで埋める。
		val padSize = PaddingUnitSize - (totalSize % PaddingUnitSize)
		val pad = new Array[Byte](PaddingUnitSize)
		if (padSize < PaddingUnitSize) {
			//0埋めのパディングを暗号化する。
			encoder.processBytes(pad, 0, padSize, buff, 0)
			//送信MACを更新する。
			egressMac.update(buff, 0, padSize)
			//ネットワークに書き込む。
			out.write(buff, 0, padSize)
		}

		//MACを計算する。
		val macBuffer = new Array[Byte](egressMac.getDigestSize)
		DigestUtils.doSum(egressMac, macBuffer)
		updateMac(egressMac, macBuffer, 0, macBuffer, 0, egress = true)
		//計算されたMACを書き込む。
		out.write(macBuffer, 0, MacSize)
	}

	/**
	 * 渡されたバイト列を復号し、MACの検証を行った上で
	 * Frameを構築して返します。
	 */
	def readFrame(buf: ByteBuf): Frame = {
		readFrame(new ByteBufInputStream(buf))
	}

	/**
	 * 渡されたバイト列を復号し、MACの検証を行った上で
	 * Frameを構築して返します。
	 */
	def readFrame(input: DataInput): Frame = {
		if (!isHeadRead) {
			//ヘッダ部分を読み取ろうとする。
			val headBuffer = new Array[Byte](32)
			try {
				input.readFully(headBuffer)
			} catch {
				case e: EOFException =>
					return null
			}
			//受信MACを更新する。
			updateMac(ingressMac, headBuffer, 0, headBuffer, 16, egress = false)
			//復号する。
			decoder.processBytes(headBuffer, 0, 16, headBuffer, 0)
			this._totalBodySize = headBuffer(0)
			this._totalBodySize = (this._totalBodySize << 8) + (headBuffer(1) & 0xFF)
			this._totalBodySize = (this._totalBodySize << 8) + (headBuffer(2) & 0xFF)
			this._isHeadRead = true
		}
		//パディングされている容量を計算する。
		var padding = PaddingUnitSize - (totalBodySize % PaddingUnitSize)
		if (padding == PaddingUnitSize) {
			padding = 0
		}

		val buffer = new Array[Byte](totalBodySize + padding + MacSize)
		try {
			//フレームの残り全体をネットワークから読み取る。
			input.readFully(buffer)
		} catch {
			case e: EOFException =>
				return null
		}
		val frameSize = buffer.length - MacSize
		//受信MACを計算する。
		ingressMac.update(buffer, 0, frameSize)
		//復号する。
		decoder.processBytes(buffer, 0, frameSize, buffer, 0)
		var pos = 0

		//RBACCodecでエンコードされているのは、最初の frame type 部分のみなので、
		//これで都合よくその部分のみが解析される。
		val decoded = RBACCodec.Decoder.decode(buffer).right.get
		val frameType: Long = decoded.asPositiveLong

		//続きは、フレームのペイロードとなるバイト列である。
		pos = decoded.pos
		val payload = new ByteArrayInputStream(buffer, pos, totalBodySize - pos)
		val size = totalBodySize - pos

		//受信MACを計算する。
		val macBuffer = new Array[Byte](ingressMac.getDigestSize)
		DigestUtils.doSum(ingressMac, macBuffer)
		updateMac(ingressMac, macBuffer, 0, buffer, frameSize, egress = false)

		this._isHeadRead = false
		new FrameCodec.Frame(frameType, size, payload)
	}

	private def updateMac(mac: KeccakDigest, seed: Array[Byte], offset: Int, out: Array[Byte], outOffset: Int, egress: Boolean): Array[Byte] = {
		//渡されたダイジェスト計算機の内部状態に基いて、256ビットダイジェスト値を計算する。
		val aesBlock = new Array[Byte](mac.getDigestSize)
		DigestUtils.doSum(mac, aesBlock)
		//計算されたダイジェスト値を、さらにAESで暗号化する。その際のAESの共通鍵は、ハンドシェイク時にECDHEで合意済みのもの。
		createMacCipherEngine.processBlock(aesBlock, 0, aesBlock, 0)

		//アルゴリズムからの出力は256ビット（＝32バイト）だが、ここでは前半の16バイトしか使わない。
		val length = MacSize
		//暗号化された出力を、渡されたシード情報とXORで混ぜる。
		for (i <- 0 until length) {
			aesBlock(i) = (aesBlock(i) ^ seed(i + offset)).toByte
		}
		//複雑な加工を施されたバイト列の前半16バイトを、さらにダイジェスト計算機に入力し、ダイジェスト値を計算する。
		mac.update(aesBlock, 0, length)
		val result = new Array[Byte](mac.getDigestSize)
		DigestUtils.doSum(mac, result)
		if (egress) {
			//送信時には、計算結果をコピーして呼び出し元に渡す。
			for (i <- 0 until length) {
				out(i + outOffset) = result(i)
			}
		} else {
			//受信時には、計算結果が呼び出し元から渡された値と合致していることを確認する。
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

	private val PaddingUnitSize = 16

	private val MacSize = 16

	/**
	 * 判明した長さを持つバイト列を表すクラスです。
	 */
	class Frame(val frameType: Long, val size: Int, val payload: InputStream) {
		def this(frameType: Int, payload: Array[Byte]) = {
			this(frameType, payload.length, new ByteArrayInputStream(payload))
		}
	}

}