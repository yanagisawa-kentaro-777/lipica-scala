package org.lipicalabs.lipica.core.validator

import org.lipicalabs.lipica.core.base.{Bloom, TransactionReceipt}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/12/28
 * @author YANAGISAWA, Kentaro
 */
object LogBloomFilterCalculator {
	def calculateLogBloomFilter(receipts: Seq[TransactionReceipt]): ImmutableBytes = {
		var result = Bloom()
		for (receipt <- receipts) {
			result = result | receipt.bloomFilter
		}
		result.immutableBytes
	}
}
