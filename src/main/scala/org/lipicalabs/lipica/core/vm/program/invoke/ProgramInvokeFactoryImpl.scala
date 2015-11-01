package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.base.{BlockChain, Block, TransactionLike, Repository}
import org.lipicalabs.lipica.core.db.BlockStore
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.Program
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/11/01 16:27
 * YANAGISAWA, Kentaro
 */
class ProgramInvokeFactoryImpl extends ProgramInvokeFactory {

	import ProgramInvokeFactoryImpl._

	private var _blockChain: BlockChain = null
	def blockChain_=(v: BlockChain): Unit = {
		this._blockChain = v
	}

	override def createProgramInvoke(tx: TransactionLike, block: Block, repository: Repository, blockStore: BlockStore) = {
		val lastBlock = this._blockChain.getBestBlock

		//現在実行中アカウントのアドレス。
		val address = if (tx.isContractCreation) tx.getContractAddress else tx.receiveAddress
		//トランザクションの開始者。これはコントラクトではあり得ない。
		val origin = tx.sendAddress
		//この実行に直接関与しているアカウントのアドレス。
		val caller = tx.sendAddress
		val balance = ImmutableBytes.asSignedByteArray(repository.getBalance(address))
		val manaPrice = tx.manaPrice
		val mana = tx.manaLimit
		val callValue = tx.value
		val data = if (tx.isContractCreation) ImmutableBytes.empty else tx.data
		val lastHash = lastBlock.hash
		val coinbase = block.coinbase
		val timestamp = block.timestamp
		val blockNumber = block.number
		val difficulty = block.difficulty
		val blockManaLimit = block.manaLimit

		ProgramInvokeImpl(address, origin, caller, balance, manaPrice, mana, callValue, data, lastHash, coinbase, timestamp, blockNumber, difficulty, blockManaLimit, repository, blockStore, byTestingSuite = false)
	}

	override def createProgramInvoke(program: Program, toAddress: DataWord, inValue: DataWord, inMana: DataWord, balanceInt: BigInt, dataIn: ImmutableBytes, repository: Repository, blockStore: BlockStore, byTestingSuite: Boolean) = {
		val address = toAddress
		val origin = program.getOriginAddress
		val caller = program.getOwnerAddress

		val balance = DataWord(balanceInt)
		val manaPrice = program.getManaPrice
		val mana = inMana
		val callValue = inValue

		val data = dataIn
		val lastHash = program.getLastHash
		val coinbase = program.getCoinbase
		val timestamp = program.getTimestamp
		val blockNumber = program.getBlockNumber
		val difficulty = program.getDifficulty
		val blockManaLimit = program.getBlockManaLimit

		ProgramInvokeImpl(address, origin, caller, balance, manaPrice, mana, callValue, data, lastHash, coinbase, timestamp, blockNumber, difficulty, blockManaLimit, repository, program.getCallDeep + 1, blockStore, byTestingSuite)
	}

}

object ProgramInvokeFactoryImpl {
	private val logger = LoggerFactory.getLogger("VM")
}