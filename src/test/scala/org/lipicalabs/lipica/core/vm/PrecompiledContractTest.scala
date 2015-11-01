package org.lipicalabs.lipica.core.vm

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class PrecompiledContractTest extends Specification {
	sequential

	private val RANDOM_CASES = 500

	"test identity (1)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000004")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = ImmutableBytes.parseHexString("112233445566")
			val result = contract.execute(data)
			val expected = "112233445566"

			result.toHexString mustEqual expected
		}
	}

	"test Sha256 (1)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = null
			val result = contract.execute(data)
			val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

			result.toHexString mustEqual expected
		}
	}

	"test Sha256 (2)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = ImmutableBytes.empty
			val result = contract.execute(data)
			val expected = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"

			result.toHexString mustEqual expected
		}
	}

	"test Sha256 (3)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000002")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = ImmutableBytes.parseHexString("112233")
			val result = contract.execute(data)
			val expected = "49ee2bf93aac3b1fb4117e59095e07abe555c3383b38d608da37680a406096e8"

			result.toHexString mustEqual expected
		}
	}

	"test ripempd160 (1)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000003")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = ImmutableBytes.parseHexString("0000000000000000000000000000000000000000000000000000000000000001")
			val result = contract.execute(data)
			val expected = "000000000000000000000000ae387fcfeb723c3f5964509af111cf5a67f30661"

			result.toHexString mustEqual expected
		}
	}

	"test ec recover (1)" should {
		"be right" in {
			val addr = DataWord("0000000000000000000000000000000000000000000000000000000000000001")
			val contract = PrecompiledContracts.getContractForAddress(addr).get
			val data = ImmutableBytes.parseHexString("18c547e4f7b0f325ad1e56f57e26c745b09a3e503d86e00e5255ff7f715d3d1c000000000000000000000000000000000000000000000000000000000000001c73b1693892219d736caba55bdb67216e485557ea6b6af75f37096c9aa6a5a75feeb940b1d03b21e36b0e47e79769f095fe2ab855bd91e3a38756b7d75a9c4549")
			val result = contract.execute(data)
			val expected = "000000000000000000000000a94f5374fce5edbc8e2a8697c15331677e6ebf0b"

			result.toHexString mustEqual expected
		}
	}

}
