package org.lipicalabs.lipica.core.crypto

import java.math.BigInteger

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class CryptoTest extends Specification {
	sequential

	"test (1)" should {
		"be right" in {
			val privateKey = new BigInteger("cd244b3015703ddf545595da06ada5516628c5feadbf49dc66049c4b370cc5d8", 16)
			val addr = ECKey.fromPrivate(privateKey).getAddress
			val expected = "89b44e4d3c81ede05d0f5de8d1a68f754d73d997"
			addr.toHexString mustEqual expected
		}
	}

	"test (2)" should {
		"be right" in {
			val key = ECKey.fromPrivate(Hex.decodeHex("a4627abc2a3c25315bff732cb22bc128f203912dd2a840f31e66efb27a47d2b1".toCharArray))

			val address = key.getAddress.toHexString
			val publicKey = Hex.encodeHexString(key.getPubKeyPoint.getXCoord.getEncoded) + Hex.encodeHexString(key.getPubKeyPoint.getYCoord.getEncoded)

			address mustEqual "47d8cb63a7965d98b547b9f0333a654b60ffa190"
			publicKey mustEqual "caa3d5086b31529bb00207eabf244a0a6c54d807d2ac0ec1f3b1bdde0dbf8130c115b1eaf62ce0f8062bcf70c0fefbc97cec79e7faffcc844a149a17fcd7bada"
		}
	}

}
