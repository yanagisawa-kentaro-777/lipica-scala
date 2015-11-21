package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.base.{Block, TransactionLike}
import org.lipicalabs.lipica.core.db.{Repository, BlockStore}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.lipicalabs.lipica.core.vm.program.Program

/**
 * Created by IntelliJ IDEA.
 * 2015/10/31 10:45
 * YANAGISAWA, Kentaro
 */
trait ProgramInvokeFactory {

	def createProgramInvoke(tx: TransactionLike, block: Block, repository: Repository, blockStore: BlockStore): ProgramInvoke

	def createProgramInvoke(program: Program, toAddress: DataWord, inValue: DataWord, inMana: DataWord, balanceInt: BigInt, dataIn: ImmutableBytes, repository: Repository, blockStore: BlockStore, byTestingSuite: Boolean): ProgramInvoke

}
