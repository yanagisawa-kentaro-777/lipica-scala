package org.lipicalabs.lipica.core.facade.manager

import java.io.{File, FileInputStream}
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.Scanner

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.kernel.ImportResult.{Exists, ImportedBest}
import org.lipicalabs.lipica.core.kernel.{Block, Blockchain}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.{ErrorLogger, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/12/23
 * @author YANAGISAWA, Kentaro
 */
class BlockLoader {

	private val logger = LoggerFactory.getLogger("general")

	/**
	 * 設定されたディレクトリ以下に存在するファイルから、
	 * ブロック情報を読み取って、渡されたチェーンに連結します。
	 */
	def loadBlocks(chain: Blockchain): Unit = {
		val path = SystemProperties.CONFIG.srcBlocksDir
		if (path.isEmpty) {
			return
		}
		val dir = Paths.get(path).toAbsolutePath.toFile
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
				ErrorLogger.logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
				logger.warn("<BlockLoader> Exception caught: %s".format(e.getClass.getSimpleName), e)
				e.printStackTrace()
		}
	}

	private def loadBlocksFromFile(src: File, chain: Blockchain): Unit = {
		withLinesInFile(src) {
			line => {
				val encodedBlockBytes = Hex.decodeHex(line.toCharArray)
				val block = Block.decode(ImmutableBytes(encodedBlockBytes))
				val bestBlock = chain.bestBlock
				if (bestBlock.blockNumber <= block.blockNumber) {
					val result = chain.tryToConnect(block)
					println("Block[%,d] %s".format(block.blockNumber, result))
					(result == ImportedBest) || (result == Exists)
				} else {
					false
				}
			}
		}
	}

	private def withLinesInFile(file: File)(proc: (String) => Boolean): Unit = {
		val inputStream = new FileInputStream(file)
		try {
			val scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name)
			var shouldContinue = true
			while (shouldContinue && scanner.hasNextLine) {
				val line = launderLine(scanner.nextLine)
				if (!line.isEmpty) {
					shouldContinue = proc(line)
				}
			}
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
