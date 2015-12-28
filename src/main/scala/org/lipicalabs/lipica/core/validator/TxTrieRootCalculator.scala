package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.TransactionLike
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.trie.TrieImpl
import org.lipicalabs.lipica.core.utils.{RBACCodec, ImmutableBytes}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/28 11:27
 * YANAGISAWA, Kentaro
 */
object TxTrieRootCalculator {

	/**
	 * トランザクションのルートを計算する根本アルゴリズム！
	 */
	def calculateTxTrieRoot(txs: Seq[TransactionLike]): ImmutableBytes = {
		val trie = new TrieImpl(null)
		if (txs.isEmpty) {
			return DigestUtils.EmptyTrieHash
		}
		txs.indices.foreach {
			i => trie.update(RBACCodec.Encoder.encode(i), txs(i).toEncodedBytes)
		}
		trie.rootHash
	}

}
