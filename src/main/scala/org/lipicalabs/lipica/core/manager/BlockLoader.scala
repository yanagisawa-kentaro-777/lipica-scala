package org.lipicalabs.lipica.core.manager

import java.io.FileInputStream
import java.nio.charset.StandardCharsets
import java.util.Scanner

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.base.{BlockHeader, Block, Blockchain}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.BlockHeaderValidator
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/12/23
 * @author YANAGISAWA, Kentaro
 */
class BlockLoader {

	private val logger = LoggerFactory.getLogger("general")

	private def headerValidator: BlockHeaderValidator = WorldManager.instance.blockHeaderValidator
	private def blockchain: Blockchain = WorldManager.instance.blockchain

	def loadBlocks(): Unit = {
		val filePath = SystemProperties.CONFIG.blocksFile
		val file = new java.io.File(filePath)
		if (filePath.isEmpty || !file.exists) {
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
					if ((0 < block.blockNumber) && !isValid(block.blockHeader)) {
						shouldContinue = false
					} else {
						blockchain.tryToConnect(block)
					}
				}
			}
		} catch {
			case e: Throwable =>
				logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
		}
	}

	private def isValid(blockHeader: BlockHeader): Boolean = {
		headerValidator.validate(blockHeader)
	}
}
