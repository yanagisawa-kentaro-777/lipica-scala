package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{Value, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/04 20:49
 * YANAGISAWA, Kentaro
 */
class SecureTrie(_db: KeyValueDataSource, _root: Value) extends TrieImpl(_db, _root) {

	def this(_db: KeyValueDataSource) = this(_db, Value.empty)

	override def get(key: ImmutableBytes): ImmutableBytes = super.get(key.sha3)

	override def update(key: ImmutableBytes, value: ImmutableBytes): Unit = super.update(key.sha3, value)

	override def delete(key: ImmutableBytes): Unit = this.update(key, ImmutableBytes.empty)

}
