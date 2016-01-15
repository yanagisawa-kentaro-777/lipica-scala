package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.crypto.digest.{DigestValue, DigestUtils}
import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * 常にキーをSHA3ダイジェスト値に変換して
 * 操作を行うTrieの実装です。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/04 20:49
 * YANAGISAWA, Kentaro
 */
class SecureTrie(_db: KeyValueDataSource, _root: DigestValue) extends TrieImpl(_db, _root) {

	def this(_db: KeyValueDataSource) = this(_db, DigestUtils.EmptyTrieHash)

	override def get(key: ImmutableBytes): ImmutableBytes = super.get(key.digest256.bytes)

	override def update(key: ImmutableBytes, value: ImmutableBytes): Unit = super.update(key.digest256.bytes, value)

	override def delete(key: ImmutableBytes): Unit = this.update(key, ImmutableBytes.empty)

}

object SecureTrie {
	def newInstance: SecureTrie = new SecureTrie(null)
}
