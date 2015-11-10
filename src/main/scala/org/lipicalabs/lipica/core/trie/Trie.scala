package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, Value}


/**
 * Merkle-Patricia Tree による trie データ構造のインターフェイスです。
 */
trait Trie {

	/**
	 * key に対応する値を取得して返します。
	 *
	 * 取得できない場合には、空のバイト列を返します。
	 */
	def get(key: ImmutableBytes): ImmutableBytes

	/**
	 * key に対して値を関連付けます。
	 */
	def update(key: ImmutableBytes, value: ImmutableBytes): Unit

	/**
	 * 指定されたkeyに対応するエントリを削除します。
	 */
	def delete(key: ImmutableBytes): Unit

	/**
	 * 最上位レベルのハッシュ値を返します。
	 */
	def rootHash: ImmutableBytes

	def root_=(value: Value): Trie

	/**
	 * 前回永続化された以降の更新を永続化します。
	 */
	def sync(): Unit

	/**
	 * 前回永続化された以降の更新を取り消します。
	 */
	def undo(): Unit

	def validate: Boolean

	def dumpToString: String

}
