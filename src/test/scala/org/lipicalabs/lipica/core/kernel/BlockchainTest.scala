package org.lipicalabs.lipica.core.kernel

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.db.datasource.HashMapDB
import org.lipicalabs.lipica.core.db.{HashMapDBFactory, RepositoryImpl, IndexedBlockStore}
import org.lipicalabs.lipica.core.facade.listener.LipicaListenerAdaptor
import org.lipicalabs.lipica.core.facade.components.{ComponentFactory, AdminInfo}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.parent_rules.{ParentNumberRule, ParentBlockHeaderValidator, DifficultyRule}
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeFactoryImpl
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner


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
			val blockStore = IndexedBlockStore.newInstance(new HashMapDB, new HashMapDB)
			val listener = new LipicaListenerAdaptor
			val genesis = Genesis.getInstance("genesis3.json")
			val repos = new RepositoryImpl(new HashMapDB, new HashMapDB, new HashMapDBFactory)

			val track = repos.startTracking
			genesis.premine.foreach {
				each => track.addBalance(each._1, each._2.balance)
			}
			track.commit()

			val blockchain = new BlockchainImpl(blockStore, repos, new Wallet, new AdminInfo, listener, ComponentFactory.createBlockValidator, ComponentFactory.createBlockHeaderValidator, new ParentBlockHeaderValidator(Seq(new ParentNumberRule, new DifficultyRule)))
			blockchain.programInvokeFactory = new ProgramInvokeFactoryImpl

			val uri = getClass.getResource("scenario1.dmp")
			val lines = java.nio.file.Files.readAllLines(Paths.get(uri.toURI), StandardCharsets.UTF_8)

			val chain = new Chain
			import scala.collection.JavaConversions._
			var root = ImmutableBytes.empty
			for (each <- lines) {
				val block = Block.decode(ImmutableBytes.parseHexString(each))
				//println("ParentHash=%s".format(block.parentHash))
				//println("Hash=%s".format(block.hash))
				val before = repos.rootHash
				val result = blockchain.tryToConnect(block)
				result mustEqual ImportResult.ImportedBest
				val after = repos.rootHash
				//println(result)
				//println("%s -> %s".format(before, after))
				root = block.stateRoot

				after mustEqual root
				chain.tryToConnect(block)
			}
			blockchain.size mustEqual 40
			repos.rootHash mustEqual root

			chain.size mustEqual blockchain.size
		}
	}

}
