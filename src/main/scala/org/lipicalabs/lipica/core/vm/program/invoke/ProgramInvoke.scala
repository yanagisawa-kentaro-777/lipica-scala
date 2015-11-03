package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.base.Repository
import org.lipicalabs.lipica.core.db.BlockStore
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:10
 * YANAGISAWA, Kentaro
 */
trait ProgramInvoke {

	/**
	 * 現在実行中アカウントのアドレス。
	 */
	def getOwnerAddress: DataWord

	/**
	 * 一連の処理の最初の実行者のアドレス。（コントラクトではあり得ない。）
	 */
	def getOriginAddress: DataWord

	/**
	 * この実行に直接関与しているアカウントのアドレス。
	 */
	def getCallerAddress: DataWord

	def getBalance: DataWord

	/**
	 * 現在の実行コンテクストのマナ価格。
	 */
	def getMinManaPrice: DataWord

	/**
	 * 現在の実行コンテクストの残マナ。
	 */
	def getMana: DataWord

	def getCallValue: DataWord

	def getDataSize: DataWord

	def getDataValue(indexData: DataWord): DataWord

	def getDataCopy(offsetData: DataWord, lengthData: DataWord): ImmutableBytes

	def getLastHash: DataWord

	def getCoinbase: DataWord

	def getTimestamp: DataWord

	def getBlockNumber: DataWord

	def getDifficulty: DataWord

	/** ブロックにおけるマナ上限。 */
	def getBlockManaLimit: DataWord

	def byTransaction: Boolean

	def byTestingSuite: Boolean

	def getCallDepth: Int

	def getRepository: Repository

	def blockStore: BlockStore

}
