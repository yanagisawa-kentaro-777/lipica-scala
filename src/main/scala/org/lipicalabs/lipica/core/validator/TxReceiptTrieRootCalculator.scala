package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.kernel.TransactionReceipt
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.trie.TrieImpl
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/12/28
 * @author YANAGISAWA, Kentaro
 */
object TxReceiptTrieRootCalculator {
	def calculateReceiptsTrieRoot(receipts: Seq[TransactionReceipt]): ImmutableBytes = {
		if (receipts.isEmpty) {
			return DigestUtils.EmptyTrieHash
		}
		val trie = TrieImpl.newInstance
		for (i <- receipts.indices) {
			val key = RBACCodec.Encoder.encode(i)
			val value = receipts(i).encode
			trie.update(key, value)
		}
		trie.rootHash
	}
}
