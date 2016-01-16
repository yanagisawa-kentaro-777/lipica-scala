package org.lipicalabs.lipica.core.validator.block_rules

import org.lipicalabs.lipica.core.kernel.{BloomFilter, TransactionReceipt}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 *
 * @since 2015/12/28
 * @author YANAGISAWA, Kentaro
 */
object LogBloomFilterCalculator {
	def calculateLogBloomFilter(receipts: Seq[TransactionReceipt]): BloomFilter = {
		var result = BloomFilter()
		for (receipt <- receipts) {
			result = result | receipt.bloomFilter
		}
		result
	}
}
