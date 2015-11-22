package org.lipicalabs.lipica.core.base

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.{ByteUtils, ImmutableBytes}
import org.lipicalabs.lipica.core.vm.{DataWord, LogInfo}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class TransactionReceiptTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val originalTx = Transaction.decode(ImmutableBytes.parseHexString("f85f800182520894000000000000000000000000000b9331677e6ebf0a801ca098ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4aa08887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3"))

			val log = LogInfo(ImmutableBytes.create(20), Seq(DataWord.apply(7777)), ImmutableBytes.parseHexString("8888"))
			val receipt = TransactionReceipt(ImmutableBytes.parseHexString("1112"), ImmutableBytes.asUnsignedByteArray(BigInt(998)), Bloom(), Seq.empty)
			receipt.transaction = originalTx
			receipt.setCumulativeMana(999)
			receipt.postTxState = ImmutableBytes.parseHexString("1111")
			receipt.setLogs(Seq(log))

			val encoded = receipt.encode
			val rebuilt = TransactionReceipt.decode(encoded)

//			val tx = rebuilt.transaction
//			tx.sendAddress.toHexString mustEqual "31bb58672e8bf7684108feeacf424ab62b873824"
//			tx.data.isEmpty mustEqual true
//			tx.manaLimit.toHexString mustEqual "5208"
//			tx.manaPrice.toHexString mustEqual "01"
//			tx.nonce.toHexString mustEqual "00"
//			tx.receiveAddress.toHexString mustEqual "000000000000000000000000000b9331677e6ebf"
//			tx.value.toHexString mustEqual "0a"
//			Hex.encodeHexString(Array(tx.signatureOption.get.v)) mustEqual "1c"
//			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a"
//			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3"

			rebuilt.cumulativeMana.toPositiveLong mustEqual 999L
			rebuilt.postTxState mustEqual ImmutableBytes.parseHexString("1111")
			rebuilt.logsAsSeq.size mustEqual 1
			rebuilt.logsAsSeq.head.topics.size mustEqual 1
			rebuilt.logsAsSeq.head.topics.head mustEqual DataWord(7777)
			rebuilt.logsAsSeq.head.data mustEqual ImmutableBytes.parseHexString("8888")
		}
	}

}
