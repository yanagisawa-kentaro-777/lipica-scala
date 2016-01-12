package org.lipicalabs.lipica.core.db

import java.io.Closeable

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * データベースが提供すべき機能を表す trait です。
 *
 * 今後キャッシュやトランザクション等の機能を実装する場合には、
 * この trait で定義することを想定しています。
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
trait Database extends Closeable {
	
	/**
	 * データベースから値を引いて返します。
	 *
	 * @param key キー。
	 * @return キーに対応する値。
	 */
	def get(key: ImmutableBytes): Option[ImmutableBytes]
	
	/**
	 * 値をデータベースに登録します。
	 *
	 * @param key キー。
	 * @param value キーに対応する値。
	 */
	def put(key: ImmutableBytes, value: ImmutableBytes): Unit

	/**
	 * 複数の値を（できれば効率良く）一括して登録します。
	 *
	 * @param rows 登録対象データ。
	 */
	def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit
	
	/**
	 * 渡されたキーに対応する値をデータベースから削除します。
	 *
	 * @param key 値を消すべきキー。
	 */
	def delete(key: ImmutableBytes): Unit

	/**
	 * 利用可能な状態に初期化します。
	 */
	def init(): Unit
	
	/**
	 * データベース接続をクローズします。
	 */
	def close(): Unit

}
