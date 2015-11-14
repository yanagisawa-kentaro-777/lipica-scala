package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:03
 * YANAGISAWA, Kentaro
 */
trait ContractDetails {
	//TODO これを実装したら、Storageにも実装を補完すること。

	//TODO メソッド名をrefactorする。

	/**
	 * アドレスを取得します。
	 * TODO
	 */
	def getAddress: ImmutableBytes

	/**
	 * アドレスを設定します。
	 */
	def setAddress(v: ImmutableBytes): Unit

	/**
	 * コードを取得します。
	 */
	def getCode: ImmutableBytes

	/**
	 * コードをセットします。
	 */
	def setCode(v: ImmutableBytes): Unit

	/**
	 * データをストレージに保存します。
	 */
	def put(key: DataWord, value: DataWord): Unit

	/**
	 * データをストレージから読み取ります。
	 * TODO
	 */
	def get(key: DataWord): DataWord

	/**
	 * ストレージデータのダイジェスト値を取得します。
	 */
	def getStorageHash: ImmutableBytes

	/**
	 * ストレージに格納されたデータ数を取得します。
	 */
	def getStorageSize: Int

	/**
	 * ストレージに保存されたデータを返します。
	 */
	def getStorage: Map[DataWord, DataWord]

	/**
	 * ストレージに保存されたデータのうち、条件に合致するものを返します。
	 */
	def getStorage(keys: Iterable[DataWord]): Map[DataWord, DataWord]


	def syncStorage(): Unit

	def getSnapshotTo(v: ImmutableBytes): ContractDetails

	/**
	 * TODO
	 */
	def getEncoded: ImmutableBytes

	def decode(data: ImmutableBytes): Unit

	def createClone: ContractDetails

	def isDirty: Boolean

	def isDirty_=(v: Boolean): Unit

	def isDeleted: Boolean

	def isDeleted_=(v: Boolean): Unit

}
