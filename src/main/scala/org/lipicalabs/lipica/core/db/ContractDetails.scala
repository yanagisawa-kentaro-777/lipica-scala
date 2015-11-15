package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * コントラクトの内容を表す trait です。
 *
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:03
 * YANAGISAWA, Kentaro
 */
trait ContractDetails {

	/**
	 * アドレスを取得します。
	 */
	def address: ImmutableBytes

	/**
	 * アドレスを設定します。
	 */
	def address_=(v: ImmutableBytes): Unit

	/**
	 * コードを取得します。
	 */
	def code: ImmutableBytes

	/**
	 * コードをセットします。
	 */
	def code_=(v: ImmutableBytes): Unit

	/**
	 * データをストレージに保存します。
	 */
	def put(key: DataWord, value: DataWord): Unit

	/**
	 * 渡されたデータをストレージに登録します。
	 */
	def put(data: Map[DataWord, DataWord]): Unit

	/**
	 * データをストレージから読み取ります。
	 */
	def get(key: DataWord): Option[DataWord]

	/**
	 * ストレージデータ全体のトップダイジェスト値を取得します。
	 */
	def storageHash: ImmutableBytes

	/**
	 * ストレージに格納されたデータ数を取得します。
	 */
	def storageSize: Int

	/**
	 * ストレージに保存されたデータを返します。
	 */
	def storageContent: Map[DataWord, DataWord]

	/**
	 * ストレージに保存されたデータのうち、条件に合致するものを返します。
	 */
	def storageContent(keys: Iterable[DataWord]): Map[DataWord, DataWord]

	/**
	 * ストレージに保存されているデータのキーの集合を返します。
	 */
	def storageKeys: Set[DataWord]

	def syncStorage(): Unit

	def getSnapshotTo(v: ImmutableBytes): ContractDetails

	/**
	 * このオブジェクトを、RBAC形式にエンコードします。
	 */
	def encode: ImmutableBytes

	/**
	 * RBAC形式にエンコードされたバイト列を解析して、このオブジェクトに属性をセットします。
	 */
	def decode(data: ImmutableBytes): Unit

	def createClone: ContractDetails

	def isDirty: Boolean

	def isDirty_=(v: Boolean): Unit

	def isDeleted: Boolean

	def isDeleted_=(v: Boolean): Unit

}
