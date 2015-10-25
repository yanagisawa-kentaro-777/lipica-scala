package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.base.Repository
import org.lipicalabs.lipica.core.db.BlockStore
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:10
 * YANAGISAWA, Kentaro
 */
trait ProgramInvoke {

	def getOwnerAddress: DataWord

	def getBalance: DataWord

	def getOriginalAddress: DataWord

	def getCallerAddress: DataWord

	def getMinManaPrice: DataWord

	def getMana: DataWord

	def getCallValue: DataWord

	def getDataSize: DataWord

	def getDataValue(indexData: DataWord): DataWord

	def getDatacopy(offsetData: DataWord, lengthData: DataWord): Array[Byte]

	def getPrevHash: DataWord

	def getCoinbase: DataWord

	def getTimestamp: DataWord

	def getNumber: DataWord

	def getDifficulty: DataWord

	def getManaLimit: DataWord

	def byTransaction: Boolean

	def byTestingSuite: Boolean

	def getCallDeep: Int

	def getRepository: Repository

	def getBlockStore: BlockStore

}
