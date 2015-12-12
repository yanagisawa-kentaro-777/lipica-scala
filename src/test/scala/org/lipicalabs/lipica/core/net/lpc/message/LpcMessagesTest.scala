package org.lipicalabs.lipica.core.net.lpc.message

import java.net.InetAddress

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
  *
  * @since 2015/12/12
  * @author YANAGISAWA, Kentaro
  */
@RunWith(classOf[JUnitRunner])
class LpcMessagesTest extends Specification {
	 sequential

	 "test BlockHashesMessage" should {
		 "be right" in {
			val original = BlockHashesMessage(Seq(ImmutableBytes.fromOneByte(1)))
			 val encoded = original.toEncodedBytes
			 val decoded: BlockHashesMessage = decodeMessage(LpcMessageCode.BlockHashes.asByte, encoded)

			 decoded.code mustEqual original.code
			 decoded.blockHashes mustEqual original.blockHashes
		 }
	 }

	"test BlocksMessage" should {
		"be right" in {
			val decoded: BlocksMessage = decodeMessage(LpcMessageCode.Blocks.asByte, ImmutableBytes.parseHexString("f901fff901fcf901f7a0fbce9f78142b5d76c2787d89d574136573f62dce21dd7bcf27c7c68ab407ccc3a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d493479415caa04a9407a2f242b2859005a379655bfb9b11a0689e7e862856d619e32ec5d949711164b447e0df7e55f4570d9fa27f33ca31a2a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000830201c008832fefd880845504456b80a05b0400eac058e0243754f4149f14e5c84cef1c33a79d83e21c80f590b953fd60881b4ef00c7a4dae1fc0c0"))

			decoded.blocks.size mustEqual 1
			decoded.code mustEqual LpcMessageCode.Blocks.asByte
			val block = decoded.blocks.head
			block.transactions.isEmpty mustEqual true
			block.blockNumber mustEqual 8
			block.hash.toHexString mustEqual "2bff4626b9854e88c72ccc5b47621a0a4e47ef5d97e1fa7c00560f7cd57543c5"
			block.stateRoot.toHexString mustEqual "689e7e862856d619e32ec5d949711164b447e0df7e55f4570d9fa27f33ca31a2"

			val encoded = decoded.toEncodedBytes
			encoded.toHexString mustEqual "f901fff901fcf901f7a0fbce9f78142b5d76c2787d89d574136573f62dce21dd7bcf27c7c68ab407ccc3a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d493479415caa04a9407a2f242b2859005a379655bfb9b11a0689e7e862856d619e32ec5d949711164b447e0df7e55f4570d9fa27f33ca31a2a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421b9010000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000830201c008832fefd880845504456b80a05b0400eac058e0243754f4149f14e5c84cef1c33a79d83e21c80f590b953fd60881b4ef00c7a4dae1fc0c0"
		}
	}

	"test GetBlockHashesByNumberMessage" should {
		"be right" in {
			val decoded: GetBlockHashesByNumberMessage = decodeMessage(LpcMessageCode.GetBlockHashesByNumber.asByte, ImmutableBytes.parseHexString("c464822710"))

			decoded.code mustEqual LpcMessageCode.GetBlockHashesByNumber.asByte
			decoded.blockNumber mustEqual 100
			decoded.maxBlocks mustEqual 10000

			decoded.toEncodedBytes.toHexString mustEqual "c464822710"
		}
	}

	"test GetBlockHashesMessage" should {
		"be right" in {
			val decoded: GetBlockHashesMessage = decodeMessage(LpcMessageCode.GetBlockHashes.asByte, ImmutableBytes.parseHexString("e4a05ad1c9caeade4cdf5798e796dc87939231d9c76c20a6a33fea6dab8e9d6dd009820100"))

			decoded.code mustEqual LpcMessageCode.GetBlockHashes.asByte
			decoded.maxBlocks mustEqual 256
			decoded.bestHash.toHexString mustEqual "5ad1c9caeade4cdf5798e796dc87939231d9c76c20a6a33fea6dab8e9d6dd009"

			decoded.toEncodedBytes.toHexString mustEqual "e4a05ad1c9caeade4cdf5798e796dc87939231d9c76c20a6a33fea6dab8e9d6dd009820100"
		}
	}


	 private def decodeMessage[T](byte: Byte, bytes: ImmutableBytes): T = {
		 (new LpcMessageFactory).create(byte, bytes).get.asInstanceOf[T]
	 }
 }
