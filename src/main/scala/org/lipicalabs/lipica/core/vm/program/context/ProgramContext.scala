package org.lipicalabs.lipica.core.vm.program.context

import org.lipicalabs.lipica.core.db.{RepositoryLike, BlockStore}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * あるコードが実行される文脈・環境を表す trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:10
 * YANAGISAWA, Kentaro
 */
trait ProgramContext {

	/**
	 * 現在実行中コードのアドレス。
	 */
	def getOwnerAddress: DataWord

	/**
	 * 一連の処理の最初の実行者のアドレス。（コントラクトではあり得ない。）
	 */
	def getOriginAddress: DataWord

	/**
	 * この処理を直接実行したアドレス。
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

	def getParentHash: DataWord

	def getCoinbase: DataWord

	def getTimestamp: DataWord

	def getBlockNumber: DataWord

	def getDifficulty: DataWord

	/** ブロックにおけるマナ上限。 */
	def getBlockManaLimit: DataWord

	def byTransaction: Boolean

	//def byTestingSuite: Boolean

	def getCallDepth: Int

	def getRepository: RepositoryLike

	def blockStore: BlockStore

}
