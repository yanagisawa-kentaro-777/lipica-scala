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
	 *
	 * Address op.
	 */
	def ownerAddress: DataWord

	/**
	 * このコード実行に至る一連の処理の
	 * 最初のトランザクションの実行者のアドレス。（コントラクトではあり得ない。）
	 *
	 * Origin op.
	 */
	def originAddress: DataWord

	/**
	 * このコードを呼び出したトランザクション実行者もしくはコントラクトのアドレス。
	 *
	 * Caller op.
	 */
	def callerAddress: DataWord

	/**
	 * コントラクト自身の残高。
	 *
	 * Balance op.
	 */
	def balance: DataWord

	/**
	 * 現在の実行コンテクストのマナ価格。
	 *
	 * ManaPrice op.
	 */
	def manaPrice: DataWord

	/**
	 * 現在の実行コンテクストのマナ消費可能容量。
	 * （トランザクションにおける上限。）
	 *
	 * Mana op.
	 */
	def manaLimit: DataWord

	/**
	 * 実行対象コントラクトに振り込まれる金額。
	 *
	 * CallValue op.
	 */
	def callValue: DataWord

	/**
	 * 実行時に渡されたデータ長。
	 *
	 * CallDataSize op.
	 */
	def dataSize: DataWord

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
	 *
	 * PrevHash op.
	 */
	def parentHash: DataWord

	/**
	 * このブロックの採掘者のアドレスを返します。
	 *
	 * Coinbase op.
	 */
	def coinbase: DataWord

	/**
	  * このブロックの生成された日時（UNIX時刻）を返します。
	 *
	 * Timestamp op.
	  */
	def timestamp: DataWord

	/**
	 * このブロックの番号を返します。
	 *
	 * BlockNumber op.
	 */
	def blockNumber: DataWord

	/**
	 * このブロックのDifficulty値を返します。
	 *
	 * Difficulty op.
	 */
	def difficulty: DataWord

	/**
	 * ブロックにおけるマナ上限を返します。
	 *
	 * ManaLimit op.
	 */
	def blockManaLimit: DataWord

	def callDepth: Int

	def repository: RepositoryLike

	def blockStore: BlockStore

	def byTransaction: Boolean
}
