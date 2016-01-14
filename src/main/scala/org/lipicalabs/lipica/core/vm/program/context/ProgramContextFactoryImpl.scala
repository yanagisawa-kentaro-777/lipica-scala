package org.lipicalabs.lipica.core.vm.program.context

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.kernel.{Blockchain, Block, TransactionLike}
import org.lipicalabs.lipica.core.db.{RepositoryLike, BlockStore}
import org.lipicalabs.lipica.core.utils.{UtilConsts, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.Program
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 16:27
 * YANAGISAWA, Kentaro
 */
class ProgramContextFactoryImpl extends ProgramContextFactory {

	import ProgramContextFactoryImpl._

	private val blockchainRef: AtomicReference[Blockchain] = new AtomicReference[Blockchain](null)
	def blockchain: Blockchain = this.blockchainRef.get
	def blockchain_=(v: Blockchain): Unit = this.blockchainRef.set(v)

	override def createProgramContext(tx: TransactionLike, block: Block, repository: RepositoryLike, blockStore: BlockStore) = {
		val lastBlock = this.blockchain.bestBlock

		//受信者もしくはコントラクトのアドレス。
		val address = if (tx.isContractCreation) tx.contractAddress.get else tx.receiverAddress
		//トランザクションの開始者。これはコントラクトではあり得ない。
		val origin = tx.senderAddress
		//この実行に直接関与しているアカウントのアドレス。
		val caller = tx.senderAddress
		val balance = ImmutableBytes.asSignedByteArray(repository.getBalance(address).getOrElse(UtilConsts.Zero))
		val manaPrice = tx.manaPrice
		val mana = tx.manaLimit
		val callValue = tx.value
		val data = if (tx.isContractCreation) ImmutableBytes.empty else tx.data
		val lastHash = lastBlock.hash
		val coinbase = block.coinbase
		val timestamp = block.timestamp
		val blockNumber = block.blockNumber
		val difficulty = block.difficulty
		val blockManaLimit = block.manaLimit

		val result = ProgramContextImpl(address, origin, caller, balance, manaPrice, mana, callValue, data, lastHash, coinbase, timestamp, blockNumber, difficulty, blockManaLimit, repository, blockStore)
		logger.info("Top level call: %s".format(result))
		result
	}

	override def createProgramContext(program: Program, toAddress: DataWord, inValue: DataWord, inMana: DataWord, balanceInt: BigInt, dataIn: ImmutableBytes, repository: RepositoryLike, blockStore: BlockStore) = {
		val address = toAddress
		val origin = program.getOriginAddress
		val caller = program.getOwnerAddress

		val balance = DataWord(balanceInt)
		val manaPrice = program.getManaPrice
		val mana = inMana
		val callValue = inValue

		val data = dataIn
		val parentHash = program.getParentHash
		val coinbase = program.getCoinbase
		val timestamp = program.getTimestamp
		val blockNumber = program.getBlockNumber
		val difficulty = program.getDifficulty
		val blockManaLimit = program.getBlockManaLimit

		val result = ProgramContextImpl(address, origin, caller, balance, manaPrice, mana, callValue, data, parentHash, coinbase, timestamp, blockNumber, difficulty, blockManaLimit, repository, program.getCallDepth + 1, blockStore)
		logger.info("Internal call: %s".format(result))
		result
	}

}

object ProgramContextFactoryImpl {
	private val logger = LoggerFactory.getLogger("vm")
}