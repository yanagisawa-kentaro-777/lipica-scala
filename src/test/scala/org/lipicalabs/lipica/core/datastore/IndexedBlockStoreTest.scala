package org.lipicalabs.lipica.core.datastore

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.kernel.{Genesis, Block}
import org.lipicalabs.lipica.core.datastore.datasource.HashMapDB
import org.lipicalabs.lipica.core.utils.{DigestValue, ImmutableBytes, UtilConsts}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class IndexedBlockStoreTest extends Specification {
	sequential

	private val blocks = new ArrayBuffer[Block]
	private var cumulativeDifficulty: BigInt = UtilConsts.Zero

	init()

	private def init(): Unit = {
		val uri = getClass.getResource("blocks.dmp")
		val lines = java.nio.file.Files.readAllLines(Paths.get(uri.toURI), StandardCharsets.UTF_8)

		val genesis = Genesis.getInstance
		append(genesis, genesis.cumulativeDifficulty)

		import scala.collection.JavaConversions._
		var i = 0
		for (line <- lines) {
			val block = Block.decode(ImmutableBytes.parseHexString(line))
			append(block, block.cumulativeDifficulty)
			i += 1
			if ((i % 1000) == 0) {
				println("Adding the %,dth block.".format(i))
			}
		}
	}

	private def append(block: Block, difficulty: BigInt): Unit = {
		this.blocks.append(block)
		this.cumulativeDifficulty += difficulty
	}

	"test (1)" should {
		"be right" in {
			this.blocks.size mustEqual 8004

			val blockStore = IndexedBlockStore.newInstance(new HashMapDB, new HashMapDB)
			var cumulativeDifficulty = UtilConsts.Zero
			for (each <- this.blocks) {
				cumulativeDifficulty += each.cumulativeDifficulty
				blockStore.saveBlock(each, cumulativeDifficulty, mainChain = true)
			}
			val bestIndex = this.blocks.last.blockNumber
			blockStore.getMaxBlockNumber mustEqual bestIndex
			blockStore.getBestBlock.get.blockNumber mustEqual bestIndex
			blockStore.getTotalDifficulty mustEqual this.cumulativeDifficulty

			Seq(0, 50, 150, 8003).foreach {
				number => privateTestBlocks(number, blockStore)
			}

			val lastBlock = this.blocks.last
			val endingHashes = blockStore.getHashesEndingWith(lastBlock.hash, 100)
			(0 until 100).foreach {
				i => {
					val block = this.blocks(8003 - i)
					val hash = endingHashes(i)
					block.hash mustEqual hash
				}
			}
			blockStore.getBlockByHash(DigestValue.parseHexString("00112233")).isEmpty mustEqual true
			blockStore.getChainBlockByNumber(8004).isEmpty mustEqual true
			blockStore.getBlocksByNumber(8004).isEmpty mustEqual true

			val indexBlock = this.blocks(7003)
			val startingHashes = blockStore.getHashesStartingWith(indexBlock.blockNumber, 100)
			(0 until 100).foreach {
				i => {
					val block = this.blocks(7003 + i)
					val hash = startingHashes(i)
					block.hash mustEqual hash
				}
			}
			ok
		}
	}

	private def privateTestBlocks(number: Long, blockStore: IndexedBlockStore): Unit = {
		val localBlock = this.blocks(number.toInt)
		val blockByHash = blockStore.getBlockByHash(localBlock.hash).get
		val chainBlock = blockStore.getChainBlockByNumber(number).get
		val headBlock = blockStore.getBlocksByNumber(number).head

		blockByHash.blockNumber mustEqual number
		chainBlock.blockNumber mustEqual number
		headBlock.blockNumber mustEqual number
	}

	//TODO test(2) 以下未実装。

}
