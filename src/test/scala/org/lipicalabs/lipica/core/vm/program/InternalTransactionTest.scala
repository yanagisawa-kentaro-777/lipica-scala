package org.lipicalabs.lipica.core.vm.program

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class InternalTransactionTest extends Specification {
	sequential


	"instance creation" should {
		"be right" in {
			val parentHash = ImmutableBytes.create(32)
			val nonce = ImmutableBytes(Array[Byte](0, 1, 2, 3, 4, 5, 6, 7))
			val manaPrice = DataWord(12L)
			val manaLimit = DataWord(100L)
			val senderAddress = ImmutableBytes(Array[Byte](7, 8, 9, 10))
			val receiverAddress = ImmutableBytes(Array[Byte](12, 13, 14, 15))
			val value = ImmutableBytes(Array[Byte](20, 21, 22, 23))
			val data = ImmutableBytes.empty

			val internalTx = new InternalTransaction(parentHash, 0, 1, nonce, manaPrice, manaLimit, senderAddress, receiverAddress, value, data, "xyz")

			internalTx.deep mustEqual 0
			internalTx.index mustEqual 1
			internalTx.nonce mustEqual nonce
			internalTx.manaPrice mustEqual manaPrice.data
			internalTx.manaLimit mustEqual manaLimit.data
			internalTx.senderAddress mustEqual senderAddress
			internalTx.receiverAddress mustEqual receiverAddress
			internalTx.value mustEqual value
			internalTx.data mustEqual data
			internalTx.note mustEqual "xyz"

			internalTx.isRejected mustEqual false
			internalTx.reject()
			internalTx.isRejected mustEqual true

			val encoded = internalTx.toEncodedBytes
			encoded.nonEmpty mustEqual true

			internalTx.toEncodedRawBytes mustEqual encoded

			try {
				internalTx.sign(ImmutableBytes.empty)
				ko
			} catch {
				case any: Throwable => ok
			}
			internalTx.signatureOption.isEmpty mustEqual true
		}
	}

}
