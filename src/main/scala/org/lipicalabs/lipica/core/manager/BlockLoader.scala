package org.lipicalabs.lipica.core.manager

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
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
		val file = new java.io.File(filePath)
		if (!file.exists) {
			return
		}
		try {
			val inputStream = new FileInputStream(filePath)
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
			println("Loaded.")
		} catch {
			case e: Throwable =>
				logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
				e.printStackTrace()
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
