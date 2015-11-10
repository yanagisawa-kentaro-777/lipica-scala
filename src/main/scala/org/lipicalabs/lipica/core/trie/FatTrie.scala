package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{Value, ImmutableBytes}

/**
 * 通常の Trie と SecureTrie とを組にして保持し各種操作を行うTrieの実装です。
 *
 * Created by IntelliJ IDEA.
 * 2015/11/05 18:36
 * YANAGISAWA, Kentaro
 */
class FatTrie(origTrieDS: KeyValueDataSource, secureTrieDS: KeyValueDataSource) extends Trie {

	private val _origTrie = new TrieImpl(origTrieDS)
	private val _secureTrie = new SecureTrie(secureTrieDS)

	def origTrie: TrieImpl = this._origTrie
	def secureTrie: SecureTrie = this._secureTrie

	override def get(key: ImmutableBytes): ImmutableBytes = this._secureTrie.get(key)

	override def update(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		this._origTrie.update(key, value)
		this._secureTrie.update(key, value)
	}

	override def delete(key: ImmutableBytes): Unit = {
		this._origTrie.delete(key)
		this._secureTrie.delete(key)
	}

	override def rootHash: ImmutableBytes = this._secureTrie.rootHash

	override def root_=(v: Value): Trie = this._secureTrie.root = v

	override def sync(): Unit = {
		this._origTrie.sync()
		this._secureTrie.sync()
	}

	override def undo(): Unit = {
		this._origTrie.undo()
		this._secureTrie.undo()
	}

	override def validate: Boolean = this._secureTrie.validate

	override def dumpToString: String = {
		this._secureTrie.dumpToString
	}

}
