package org.lipicalabs.lipica.core.base

import org.lipicalabs.lipica.core.utils.ImmutableBytes

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
