package org.lipicalabs.lipica.core.kernel

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class BloomFilterTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val address = Hex.decodeHex("095e7baea6a6c7c4c2dfeb977efac326af552d87".toCharArray)
			val addressBloom = BloomFilter.createFromDigest(ImmutableBytes(DigestUtils.digest256(address)))

			val topic = Hex.decodeHex("0000000000000000000000000000000000000000000000000000000000000000".toCharArray)
			val topicBloom = BloomFilter.createFromDigest(ImmutableBytes(DigestUtils.digest256(topic)))

			val answer = "00000000000000001000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000020000000000040000000000000000000000000000000000000000000000000000000"

			val totalBloom = BloomFilter() | addressBloom | topicBloom
			totalBloom.toString mustEqual answer
		}
	}


}
