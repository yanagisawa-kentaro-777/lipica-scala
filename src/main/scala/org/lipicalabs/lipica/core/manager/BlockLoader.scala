package org.lipicalabs.lipica.core.manager

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.Scanner

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.base.ImportResult.{Exists, ImportedBest}
import org.lipicalabs.lipica.core.base.{Block, Blockchain}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/12/23
 * @author YANAGISAWA, Kentaro
 */
class BlockLoader {

	private val logger = LoggerFactory.getLogger("general")

	def loadBlocks(chain: Blockchain): Unit = {
		val filePath = SystemProperties.CONFIG.blocksFile
		if (filePath.isEmpty) {
			return
		}
		val dir = new java.io.File(filePath)
		if (!dir.exists) {
			return
		}
		try {
			val files = dir.listFiles.sortWith((f1, f2) => f1.getName.compareTo(f2.getName) < 0)
			for (file <- files) {
				loadBlocksFromFile(file, chain)
			}
		} catch {
			case e: Throwable =>
				logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
				e.printStackTrace()
		}
	}

	private def loadBlocksFromFile(src: File, chain: Blockchain): Unit = {
		println("Loading from file: %s".format(src))
		val inputStream = new FileInputStream(src)
		try {
			val scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name)
			var shouldContinue = true
			while (shouldContinue && scanner.hasNextLine) {
				val line = launderLine(scanner.nextLine)
				if (!line.isEmpty) {
					val encodedBlockBytes = Hex.decodeHex(line.toCharArray)
					val block = Block.decode(ImmutableBytes(encodedBlockBytes))
					val bestBlock = chain.bestBlock
					if (bestBlock.blockNumber <= block.blockNumber) {
						val result = chain.tryToConnect(block)
						println("Block[%,d] %s".format(block.blockNumber, result))
						shouldContinue = ((result == ImportedBest) || (result == Exists))
					}
				}
			}
			println("Loaded from file: %s.".format(src))
		} finally {
			inputStream.close()
		}
	}

	private def launderLine(line: String): String = {
		if (line eq null) {
			""
		} else {
			line.trim
		}
	}

}
