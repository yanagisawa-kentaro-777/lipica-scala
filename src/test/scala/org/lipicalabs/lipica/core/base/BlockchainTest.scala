package org.lipicalabs.lipica.core.base

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.ImportResult
import org.lipicalabs.lipica.core.db.datasource.HashMapDB
import org.lipicalabs.lipica.core.db.{RepositoryImpl, BlockInfo, IndexedBlockStore}
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.manager.AdminInfo
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.ParentBlockHeaderValidator
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeFactoryImpl
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class BlockchainTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val blockStore = new IndexedBlockStore(new mutable.HashMap[Long, Seq[BlockInfo]], new HashMapDB, null, null)
			val listener = new LipicaListener {
				override def onBlock(block: Block, receipts: Iterable[TransactionReceipt]) = ()
				override def onTransactionExecuted(summary: TransactionExecutionSummary) = ()
				override def trace(s: String) = ()
				override def onPendingTransactionsReceived(transactions: Iterable[TransactionLike]) = ()
				override def onVMTraceCreated(txHash: String, trace: String) = ()
			}
			val genesis = Genesis.getInstance("genesis3.json")
			val repos = new RepositoryImpl(new HashMapDB, new HashMapDB)
			genesis.premine.foreach {
				each => repos.addBalance(each._1, each._2.balance)
			}

			val blockchain = new BlockchainImpl(blockStore, repos, new Wallet, new AdminInfo, listener, new ParentBlockHeaderValidator(Seq.empty))
			blockchain.programInvokeFactory = new ProgramInvokeFactoryImpl

			val uri = getClass.getResource("scenario1.dmp")
			val lines = java.nio.file.Files.readAllLines(Paths.get(uri.toURI), StandardCharsets.UTF_8)

			import scala.collection.JavaConversions._
			var root = ImmutableBytes.empty
			for (each <- lines) {
				val block = Block.decode(ImmutableBytes.parseHexString(each))
				//println("ParentHash=%s".format(block.parentHash))
				//println("Hash=%s".format(block.hash))
				val result = blockchain.tryToConnect(block)
				result mustEqual ImportResult.ImportedBest
				//println(result)
				root = block.stateRoot
			}
			blockchain.size mustEqual 40
			repos.getRoot mustEqual root
		}
	}

}
