package org.lipicalabs.lipica.core.validator

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class ProofOfWorkRuleTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val encoded = "f9021af90215a0809870664d9a43cf1827aa515de6374e2fad1bf64290a9f261dd49c525d6a0efa01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d4934794f927a40c8b7f6e07c5af7fa2155b4864a4112b13a010c8ec4f62ecea600c616443bcf527d97e5b1c5bb4a9769c496d1bf32636c95da056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b901000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000086015a1c28ae5e82bf958302472c808455c4e47b99476574682f76312e302e312f6c696e75782f676f312e342e32a0788ac534cb2f6a226a01535e29b11a96602d447aed972463b5cbcc7dd5d633f288e2ff1b6435006517c0c0"
			val block = Block.decode(ImmutableBytes.parseHexString(encoded))

			block.blockHeader.getProofOfWorkBoundary mustEqual ImmutableBytes.parseHexString("0000000000bd59a74a8619f14c3d793747f1989a29ed6c83a5a488bac185679b")
			block.blockHeader.calculateProofOfWorkValue mustEqual ImmutableBytes.parseHexString("000000000017f78925469f2f18fe7866ef6d3ed28d36fb013bc93d081e05809c")

			val rule = new ProofOfWorkRule
			rule.validate(block.blockHeader) mustEqual true
		}
	}

}