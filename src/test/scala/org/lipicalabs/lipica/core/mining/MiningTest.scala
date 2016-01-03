package org.lipicalabs.lipica.core.mining

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.base.{Block, BlockHeader}
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.validator.ProofOfWorkRule
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class MiningTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val bytes = new Array[Byte](32).map(_ => 0xFF.toByte)
			//1足したら、オーバーフローしてゼロに戻るはず。
			Miner.increment(bytes) mustEqual false
			bytes mustEqual new Array[Byte](32)
			//今度は大丈夫なはず。
			Miner.increment(bytes) mustEqual true
		}
	}

	"test (2)" should {
		"be right" in {
			val encoded = "f9021af90215a0809870664d9a43cf1827aa515de6374e2fad1bf64290a9f261dd49c525d6a0efa01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d4934794f927a40c8b7f6e07c5af7fa2155b4864a4112b13a010c8ec4f62ecea600c616443bcf527d97e5b1c5bb4a9769c496d1bf32636c95da056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b901000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000086015a1c28ae5e82bf958302472c808455c4e47b99476574682f76312e302e312f6c696e75782f676f312e342e32a0788ac534cb2f6a226a01535e29b11a96602d447aed972463b5cbcc7dd5d633f288e2ff1b6435006517c0c0"
			val block = Block.decode(ImmutableBytes.parseHexString(encoded))
			val rule = new ProofOfWorkRule
			rule.validate(block.blockHeader) mustEqual true
		}
	}

}
