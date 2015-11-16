package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/10/25 14:04
 * YANAGISAWA, Kentaro
 */
trait Block {
	//TODO

	def number: Long

	def hash: ImmutableBytes

	def coinbase: ImmutableBytes

	def timestamp: Long

	def difficulty: ImmutableBytes

	def manaLimit: Long
}

class PlainBlock(header: BlockHeader, transactions: Seq[TransactionLike], uncles: Seq[BlockHeader]) extends Block {

	import Block._

	override def number = ???

	override def coinbase = ???

	override def manaLimit = ???

	override def hash = ???

	override def difficulty = ???

	override def timestamp = ???
}

object Block {
	private val logger = LoggerFactory.getLogger(getClass)

	val BlockReward =
		if (SystemProperties.CONFIG.isFrontier) {
			BigInt("5000000000000000000")
		} else {
			BigInt("1500000000000000000")
		}

	val UncleReward = BlockReward * BigInt(15) / BigInt(16)

	val InclusionReward = BlockReward / BigInt(32)
}