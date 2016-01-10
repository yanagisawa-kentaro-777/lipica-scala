package org.lipicalabs.lipica.core.kernel

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/22 10:42
 * YANAGISAWA, Kentaro
 */
class Chain {

	import Chain._

	private val chain: mutable.Buffer[Block] = new ArrayBuffer[Block]

	private var _totalDifficulty: BigInt = UtilConsts.Zero
	def totalDifficulty: BigInt = this._totalDifficulty

	private val index: mutable.Map[ImmutableBytes, Block] = new mutable.HashMap[ImmutableBytes, Block]

	/**
	 * 渡されたブロックがこのチェーンの直接の後続ブロックであれば、連結します。
	 */
	def tryToConnect(block: Block): Boolean = {
		if (this.chain.isEmpty) {
			append(block)
			true
		} else {
			val lastBlock = this.chain.last
			if (lastBlock.isParentOf(block)) {
				append(block)
				true
			} else {
				false
			}
		}
	}

	/**
	 * このチェーンの末尾に、渡されたブロックを追加します。
	 */
	def append(block: Block): Unit = {
		logger.info("<Chain> Adding a block to the chain: %s".format(block.shortHash))
		this._totalDifficulty += block.cumulativeDifficulty
		logger.info("<Chain> Total difficulty on the chain is: %s".format(this._totalDifficulty))
		this.chain.append(block)
		this.index.put(block.hash, block)
	}

	def get(idx: Int): Block = this.chain(idx)

	def size: Int = this.chain.size

	def lastOption: Option[Block] = this.chain.lastOption

	def last: Block = this.chain.last

}

object Chain {
	private val logger = LoggerFactory.getLogger("blockchain")
}