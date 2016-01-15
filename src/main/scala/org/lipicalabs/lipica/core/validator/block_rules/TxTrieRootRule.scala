package org.lipicalabs.lipica.core.validator.block_rules

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.crypto.digest.{DigestValue, DigestUtils}
import org.lipicalabs.lipica.core.kernel.{Block, TransactionLike}
import org.lipicalabs.lipica.core.trie.TrieImpl
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/12/28 11:27
 * YANAGISAWA, Kentaro
 */
class TxTrieRootRule extends BlockRule {

	import TxTrieRootRule._

	override def validate(block: Block): Boolean = {
		errors.clear()
		val calculated = calculateTxTrieRoot(block.transactions)
		val ok = calculated == block.blockHeader.txTrieRoot
		if (!ok) {
			errors.append("BAD TX ROOT HASH %s != %s".format(calculated, block.blockHeader.txTrieRoot))
		}
		ok
	}

}

object TxTrieRootRule {

	/**
	 * トランザクションのルートを計算する根本アルゴリズム！
	 */
	def calculateTxTrieRoot(txs: Seq[TransactionLike]): DigestValue = {
		if (txs.isEmpty) {
			return DigestUtils.EmptyTrieHash
		}
		val trie = TrieImpl.newInstance
		txs.indices.foreach {
			i => trie.update(RBACCodec.Encoder.encode(i), txs(i).toEncodedBytes)
		}
		trie.rootHash
	}

}
