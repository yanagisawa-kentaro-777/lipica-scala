package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.utils.Value


/**
 * Merkle-Patricia Tree による trie データ構造のインターフェイスです。
 */
trait Trie {

	/**
	 * key に対応する値を取得して返します。
	 *
	 * 取得できない場合には、空のバイト列を返します。
	 */
	def get(key: Array[Byte]): Array[Byte]

	/**
	 * key に対して値を関連付けます。
	 */
	def update(key: Array[Byte], value: Array[Byte]): Unit

	/**
	 * 指定されたkeyに対応するエントリを削除します。
	 */
	def delete(key: Array[Byte]): Unit

	/**
	 * 最上位レベルのハッシュ値を返します。
	 */
	def rootHash: Array[Byte]

	def root(value: Value): Trie

	/**
	 * 前回永続化された以降の更新を永続化します。
	 */
	def sync(): Unit

	/**
	 * 前回永続化された以降の更新を取り消します。
	 */
	def undo(): Unit

	def validate: Boolean

}
