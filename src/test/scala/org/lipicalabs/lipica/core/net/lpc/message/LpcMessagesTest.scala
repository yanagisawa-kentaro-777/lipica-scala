package org.lipicalabs.lipica.core.net.lpc.message

import java.net.InetAddress

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.client.Capability
import org.lipicalabs.lipica.core.net.lpc.LpcMessageCode
import org.lipicalabs.lipica.core.net.message.ReasonCode
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


	 private def decodeMessage[T](byte: Byte, bytes: ImmutableBytes): T = {
		 (new LpcMessageFactory).create(byte, bytes).get.asInstanceOf[T]
	 }
 }
