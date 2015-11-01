package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:08
 * YANAGISAWA, Kentaro
 */
trait BlockStore {
	//TODO

	/**
	 * ブロック番号で、ブロックハッシュを引いて返します。
	 * フォークが発生している場合には、渡された枝の祖先に当たるものを返します。
	 */
	def getBlockHashByNumber(blockNumber: Long, branchBlockHash: ImmutableBytes): ImmutableBytes
}
