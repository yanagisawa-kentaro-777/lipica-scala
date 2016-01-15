package org.lipicalabs.lipica.core.datastore.datasource

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * バイト配列をキーおよび値とするデータ永続化機構が実装すべき trait です。
 *
 * よりアプリケーション寄りのデータストアコンポーネントのバックエンドとして動作することを想定しています。
 */
trait KeyValueDataSource extends DataSource {

	/**
	 * 指定されたキーに対応する値を返します。
	 *
	 * @param key キー。
	 * @return 対応する値があればその値、なければ None。
	 */
	def get(key: ImmutableBytes): Option[ImmutableBytes]

	/**
	 * 指定されたキーに指定された値を結びつけて保存します。
	 *
	 * @param key キー。
	 * @param value 値。
	 * @return 以前に結び付けられていた値があれば、その値。
	 */
	def put(key: ImmutableBytes, value: ImmutableBytes): Unit

	/**
	 * 指定されたキーおよび値の連想配列の要素を一括して、
	 * 可能ならば効率よく、登録します。
	 *
	 * @param rows キーおよび値の連想配列。
	 */
	def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit

	/**
	 * 指定されたキーおよびそれに対応する値を削除します。
	 *
	 * @param key キー。
	 */
	def delete(key: ImmutableBytes): Unit

	/**
	 * すべての要素を削除します。
	 *
	 * 多くの実装において、この操作は非常に高負荷であると予想されますので、
	 * みだりに呼び出さないようにしてください。
	 */
	def deleteAll(): Unit

	/**
	 * すべてのキーの集合を返します。
	 *
	 * 多くの実装において、この操作は非常に高負荷であると予想されますので、
	 * みだりに呼び出さないようにしてください。
	 */
	def keys: Set[ImmutableBytes]

}
