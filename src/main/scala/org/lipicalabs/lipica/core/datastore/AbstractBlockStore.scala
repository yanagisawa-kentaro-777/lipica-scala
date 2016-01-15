package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.crypto.digest.DigestValue
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Created by IntelliJ IDEA.
 * 2015/11/19 20:27
 * YANAGISAWA, Kentaro
 */
abstract class AbstractBlockStore extends BlockStore {

	override def getBlockHashByNumber(aBlockNumber: Long, aBranchBlockHash: DigestValue): Option[DigestValue] = {
		getBlockByHash(aBranchBlockHash) match {
			case Some(branchBlock) =>
				if (branchBlock.blockNumber < aBlockNumber) {
					//このブロックの祖先が、渡された番号であるということはあり得ない。
					throw new IllegalAccessException("<AbstractBlockStore> Requested block number is greater than branch hash number: %d < %d".format(branchBlock.blockNumber, aBlockNumber))
				}
				var result = branchBlock
				while (aBlockNumber < result.blockNumber) {
					getBlockByHash(result.parentHash) match {
						case Some(b) => result = b
						case _ => return None
					}
				}
				Option(result.hash)
			case _ => None
		}
	}

}
