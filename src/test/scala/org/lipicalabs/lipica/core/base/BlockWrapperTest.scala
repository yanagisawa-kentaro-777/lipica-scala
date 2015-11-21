package org.lipicalabs.lipica.core.base

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class BlockWrapperTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val encoded = "f901f8f901f3a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a09178d0f23c965d81f0834a4c72c6253ce6830f4022b1359aaebfc1ecba442d4ea056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b90100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000008302000080832fefd8808080a0000000000000000000000000000000000000000000000000000000000000000088000000000000002ac0c0"
			val block = Block.decode(ImmutableBytes.parseHexString(encoded))
			val nodeId = ImmutableBytes.parseHexString("00010203")

			val wrapper1 = BlockWrapper(block, newBlock = false, nodeId = nodeId)
			wrapper1.blockNumber mustEqual block.blockNumber
			wrapper1.hash mustEqual block.hash
			wrapper1.parentHash mustEqual block.parentHash
			wrapper1.shortHash mustEqual block.shortHash
			wrapper1.nodeId mustEqual nodeId
			wrapper1.block.isEqualTo(block) mustEqual true
			wrapper1.importFailedAt mustEqual 0
			wrapper1.receivedAt mustEqual 0
			wrapper1.isSolidBlock mustEqual true
			wrapper1.encode mustEqual block.encode

			val encodedBytes = wrapper1.toBytes
			val decodedWrapper = BlockWrapper.parse(encodedBytes)
			decodedWrapper mustEqual wrapper1
		}
	}


}
