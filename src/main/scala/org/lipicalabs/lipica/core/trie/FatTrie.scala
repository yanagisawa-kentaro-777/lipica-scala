package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.{Value, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/05 18:36
 * YANAGISAWA, Kentaro
 */
class FatTrie(origTrieDS: KeyValueDataSource, secureTrieDS: KeyValueDataSource) extends Trie {

	private val origTrie = new TrieImpl(origTrieDS)
	private val secureTrie = new SecureTrie(secureTrieDS)

	def getOrigTrie: TrieImpl = this.origTrie
	def getSecureTrie: SecureTrie = this.secureTrie

	override def get(key: ImmutableBytes): ImmutableBytes = this.secureTrie.get(key)

	override def update(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		this.origTrie.update(key, value)
		this.secureTrie.update(key, value)
	}

	override def delete(key: ImmutableBytes): Unit = {
		this.origTrie.delete(key)
		this.secureTrie.delete(key)
	}

	override def rootHash: ImmutableBytes = this.secureTrie.rootHash

	override def root(v: Value): Trie = this.secureTrie.root(v)

	override def sync(): Unit = {
		this.origTrie.sync()
		this.secureTrie.sync()
	}

	override def undo(): Unit = {
		this.origTrie.undo()
		this.secureTrie.undo()
	}

	override def validate: Boolean = this.secureTrie.validate

//	override def getTrieDump: String = {
//		this.secureTrie.getTrieDump
//	}


}
