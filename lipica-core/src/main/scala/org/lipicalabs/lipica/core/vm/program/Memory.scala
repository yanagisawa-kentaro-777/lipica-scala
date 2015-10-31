package org.lipicalabs.lipica.core.vm.program

import org.lipicalabs.lipica.core.utils.ByteUtils
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.listener.{ProgramListenerAware, ProgramListener}

import scala.collection.mutable

/**
 * Lipica VMのメモリを表すクラスです。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class Memory extends ProgramListenerAware {

	import scala.collection.JavaConversions._
	import Memory._

	private val chunks: mutable.Buffer[Array[Byte]] = asScalaBuffer(new java.util.LinkedList[Array[Byte]])
	private var softSize = 0
	private var traceListener: ProgramListener = null

	override def setTraceListener(traceListener: ProgramListener): Unit = {
		this.traceListener = traceListener
	}

	/**
	 * メモリを追加的に確保します。
	 */
	def extend(address: Int, size: Int): Unit = {
		if (size <= 0) return
		val newSize = address + size
		val toAllocate1 = newSize - internalSize
		if (0 < newSize - internalSize) {
			addChunks(java.lang.Math.ceil(toAllocate1.toDouble / CHUNK_SIZE.toDouble).toInt)
		}
		var toAllocate2 = newSize - softSize
		if (0 < toAllocate2) {
			toAllocate2 = java.lang.Math.ceil(toAllocate2.toDouble / WORD_SIZE.toDouble).toInt * WORD_SIZE
			this.softSize += toAllocate2

			if (this.traceListener ne null) {
				this.traceListener.onMemoryExtend(toAllocate2)
			}
		}
	}

	/**
	 * 渡された番地から始めて、渡されたサイズのデータを読み取って返します。
	 */
	def read(address: Int, size: Int): Array[Byte] = {
		if (size <= 0) return Array.emptyByteArray

		extend(address, size)

		val data = new Array[Byte](size)
		var (chunkIndex, chunkOffset) = computeChunkIndexAndOffset(address)

		var toGrab = data.length
		var start = 0

		while (0 < toGrab) {
			val copied = grabMax(chunkIndex, chunkOffset, toGrab, data, start)
			chunkIndex += 1
			chunkOffset = 0

			toGrab -= copied
			start += copied
		}
		data
	}

	/**
	 * 渡された番地に対して、渡されたバイト配列の最初から、指定されたバイト数分だけ書き込みます。
	 */
	def write(address: Int, data: Array[Byte], aDataSize: Int, limited: Boolean): Unit = {
		val dataSize = if (data.length < aDataSize) data.length else aDataSize

		if (!limited) extend(address, dataSize)

		var (chunkIndex, chunkOffset) = computeChunkIndexAndOffset(address)

		var toCapture =
			if (limited) {
				if (softSize < (address + dataSize)) softSize - address else dataSize
			} else {
				dataSize
			}

		var start = 0
		while (0 < toCapture) {
			val captured = captureMax(chunkIndex, chunkOffset, toCapture, data, start)
			chunkIndex += 1
			chunkOffset = 0
			toCapture -= captured
			start += captured
		}

		if (traceListener ne null) {
			traceListener.onMemoryWrite(address, data, dataSize)
		}
	}

	/**
	 * メモリを指定された容量だけ確保してから、
	 * 指定された位置に渡されたデータを書き込みます。
	 */
	def extendAndWrite(address: Int, allocSize: Int, data: Array[Byte]): Unit = {
		extend(address, allocSize)
		write(address, data, data.length, limited = false)
	}

	/**
	 * メモリ上の番地から１バイトを読み取って返します。
	 */
	def readByte(address: Int): Byte = {
		val (chunkIndex, chunkOffset) = computeChunkIndexAndOffset(address)

		val chunk = this.chunks.get(chunkIndex)
		chunk(chunkOffset)
	}

	/**
	 * メモリ上の番地から１ワードを読み取って返します。
	 */
	def readWord(address: Int): DataWord = {
		DataWord(read(address, 32))
	}

	def size: Int = this.softSize

	def internalSize: Int = this.chunks.size * CHUNK_SIZE

	def chunksAsSeq: Seq[Array[Byte]] = this.chunks.toSeq

	override def toString: String = {
		val memoryData = new StringBuilder
		val firstLine = new StringBuilder
		val secondLine = new StringBuilder

		(0 until softSize).foreach {i => {
			val value = readByte(i)
			val character = if (0x20.toByte <= value && value <= 0x7e.toByte) new String(Array[Byte](value)) else "?"
			firstLine.append(character).append("")
			secondLine.append(ByteUtils.oneByteToHexString(value)).append(" ")
			if ((i + 1) % 8 == 0) {
				val tmp = String.format("%4s", Integer.toString(i - 7, 16)).replace(" ", "0")
				memoryData.append("").append(tmp).append(" ")
				memoryData.append(firstLine).append(" ")
				memoryData.append(secondLine)
				if (i + 1 < softSize) memoryData.append("\n")
				firstLine.setLength(0)
				secondLine.setLength(0)
			}
		}}
		memoryData.toString()
	}

	private def computeChunkIndexAndOffset(address: Int): (Int, Int) = {
		val chunkIndex = address / CHUNK_SIZE
		val chunkOffset = address % CHUNK_SIZE
		(chunkIndex, chunkOffset)
	}


	private def addChunks(num: Int): Unit = {
		(0 until num).foreach {_ =>
			chunks.append(new Array[Byte](CHUNK_SIZE))
		}
	}

	private def captureMax(chunkIndex: Int, chunkOffset: Int, size: Int, src: Array[Byte], srcPos: Int): Int = {
		val chunk = chunks.get(chunkIndex)
		val toCapture = java.lang.Math.min(size, chunk.length - chunkOffset)
		System.arraycopy(src, srcPos, chunk, chunkOffset, toCapture)
		toCapture
	}

	private def grabMax(chunkIndex: Int, chunkOffset: Int, size: Int, dest: Array[Byte], destPos: Int): Int = {
		val chunk = chunks.get(chunkIndex)
		val toGrab = java.lang.Math.min(size, chunk.length - chunkOffset)
		System.arraycopy(chunk, chunkOffset, dest, destPos, toGrab)
		toGrab
	}

}

object Memory {
	private[program] val CHUNK_SIZE = 1024
	private[program] val WORD_SIZE = DataWord.NUM_BYTES
}