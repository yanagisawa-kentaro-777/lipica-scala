package org.lipicalabs.lipica.core.manager

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.Scanner

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.base.ImportResult.ImportedBest
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

	private def blockchain: Blockchain = WorldManager.instance.blockchain

	def loadBlocks(): Unit = {
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
				val encodedBlockBytes = Hex.decodeHex(scanner.nextLine.toCharArray)
				val block = Block.decode(ImmutableBytes(encodedBlockBytes))
				if (blockchain.bestBlock.blockNumber <= block.blockNumber) {
					val result = blockchain.tryToConnect(block)
					println("Block[%,d] %s".format(block.blockNumber, result))
					shouldContinue = result == ImportedBest
				}
			}
		} catch {
			case e: Throwable =>
				logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}

}
