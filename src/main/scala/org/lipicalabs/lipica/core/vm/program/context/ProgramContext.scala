package org.lipicalabs.lipica.core.vm.program.context

import org.lipicalabs.lipica.core.datastore.{RepositoryLike, BlockStore}
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
	 * 現在実行中コード自身のアドレス。
	 * （コントラクト作成の場合も、コントラきうと呼び出しの場合も。）
	 */
	def getOwnerAddress: DataWord

	/**
	 * このコード実行に至る一連の処理の
	 * 最初のトランザクションの実行者のアドレス。（コントラクトではあり得ない。）
	 */
	def getOriginAddress: DataWord

	/**
	 * このコードを呼び出したトランザクション実行者もしくはコントラクトのアドレス。
	 */
	def getCallerAddress: DataWord

	/**
	 * コントラクト自身の残高。
	 */
	def getBalance: DataWord

	/**
	 * 現在の実行コンテクストのマナ価格。
	 */
	def getMinManaPrice: DataWord

	/**
	 * 現在の実行コンテクストのマナ消費可能容量。
	 */
	def getMana: DataWord

	/**
	 * 実行対象コントラクトに振り込まれる金額。
	 */
	def getCallValue: DataWord

	/**
	 * 実行時に渡されたデータ長。
	 */
	def getDataSize: DataWord

	/**
	 * 指定されたオフセットから、１ワード分のデータをスタックに格納します。
	 */
	def getDataValue(indexData: DataWord): DataWord

	/**
	 * 指定されたオフセットから、指定された長さのデータを返します。
	 */
	def getDataCopy(offsetData: DataWord, lengthData: DataWord): ImmutableBytes

	/**
	 * このブロックの直前ブロックのハッシュ値を返します。
	 */
	def getParentHash: DataWord

	/**
	 * このブロックの採掘者のアドレスを返します。
	 */
	def getCoinbase: DataWord

	/**
	  * このブロックの生成された日時（UNIX時刻）を返します。
	  */
	def getTimestamp: DataWord

	/**
	 * このブロックの番号を返します。
	 */
	def getBlockNumber: DataWord

	/**
	 * このブロックのDifficulty値を返します。
	 */
	def getDifficulty: DataWord

	/**
	 * ブロックにおけるマナ上限を返します。
	 */
	def getBlockManaLimit: DataWord

	def byTransaction: Boolean


	def getCallDepth: Int

	def getRepository: RepositoryLike

	def blockStore: BlockStore

}
