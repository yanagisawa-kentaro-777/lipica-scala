package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.base.genesis.GenesisLoader
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:56
 * YANAGISAWA, Kentaro
 */
class Genesis private[base](_header: BlockHeader, val premine: Map[ImmutableBytes, AccountState]) extends PlainBlock(_header, Seq.empty[TransactionLike], Seq.empty[BlockHeader]) {
	//
}

object Genesis {

	//TODO Genesisの属性変更を不可能にする。

	/**
	 * Genesisブロックのインスタンスを返します。
	 * （可変の要素が多々あるので、毎回ロードする。）
	 */
	def getInstance: Genesis = GenesisLoader.loadGenesisBlock

	val GenesisHash: ImmutableBytes = getInstance.hash

}