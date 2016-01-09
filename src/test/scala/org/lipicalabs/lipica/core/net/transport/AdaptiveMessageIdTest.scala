package org.lipicalabs.lipica.core.net.transport

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.Capability
import org.lipicalabs.lipica.core.net.lpc.{LpcVersion, LpcMessageCode}
import org.lipicalabs.lipica.core.net.p2p.P2PMessageCode
import org.lipicalabs.lipica.core.net.shh.ShhMessageCode
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class AdaptiveMessageIdTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			P2PMessageCode.all.size mustEqual 7

			val resolver = new MessageCodesResolver(Seq.empty)
			resolver.withP2POffset(P2PMessageCode.Hello.asByte) mustEqual 0
			resolver.withP2POffset(P2PMessageCode.Disconnect.asByte) mustEqual 1
			resolver.withP2POffset(P2PMessageCode.Ping.asByte) mustEqual 2
			resolver.withP2POffset(P2PMessageCode.Pong.asByte) mustEqual 3
			resolver.withP2POffset(P2PMessageCode.GetPeers.asByte) mustEqual 4
			resolver.withP2POffset(P2PMessageCode.Peers.asByte) mustEqual 5
			resolver.withP2POffset(P2PMessageCode.User.asByte) mustEqual 15
		}
	}

	"test (2)" should {
		"be right" in {
			LpcMessageCode.all.size mustEqual 9

			val resolver = new MessageCodesResolver(Seq.empty)
			resolver.setLpcOffset(0x10)

			resolver.withLpcOffset(LpcMessageCode.Status.asByte) mustEqual 0x10 + 0
			resolver.withLpcOffset(LpcMessageCode.NewBlockHashes.asByte) mustEqual 0x10 + 1
			resolver.withLpcOffset(LpcMessageCode.Transactions.asByte) mustEqual 0x10 + 2
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashes.asByte) mustEqual 0x10 + 3
			resolver.withLpcOffset(LpcMessageCode.BlockHashes.asByte) mustEqual 0x10 + 4
			resolver.withLpcOffset(LpcMessageCode.GetBlocks.asByte) mustEqual 0x10 + 5
			resolver.withLpcOffset(LpcMessageCode.Blocks.asByte) mustEqual 0x10 + 6
			resolver.withLpcOffset(LpcMessageCode.NewBlock.asByte) mustEqual 0x10 + 7
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashesByNumber.asByte) mustEqual 0x10 + 8
		}
	}

	"test (3)" should {
		"be right" in {
			ShhMessageCode.all.size mustEqual 5

			val resolver = new MessageCodesResolver(Seq.empty)
			resolver.setShhOffset(0x20)

			resolver.withShhOffset(ShhMessageCode.Status.asByte) mustEqual 0x20 + 0
			resolver.withShhOffset(ShhMessageCode.Message.asByte) mustEqual 0x20 + 1
			resolver.withShhOffset(ShhMessageCode.AddFilter.asByte) mustEqual 0x20 + 2
			resolver.withShhOffset(ShhMessageCode.RemoveFilter.asByte) mustEqual 0x20 + 3
			resolver.withShhOffset(ShhMessageCode.PacketCount.asByte) mustEqual 0x20 + 4
		}
	}

	"test (4)" should {
		"be right" in {
			val resolver = new MessageCodesResolver(Seq(new Capability(Capability.LPC, LpcVersion.SupportedVersions.last.toByte), new Capability(Capability.SHH, 0)))

			resolver.withLpcOffset(LpcMessageCode.Status.asByte) mustEqual 0x10 + 0
			resolver.withLpcOffset(LpcMessageCode.NewBlockHashes.asByte) mustEqual 0x10 + 1
			resolver.withLpcOffset(LpcMessageCode.Transactions.asByte) mustEqual 0x10 + 2
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashes.asByte) mustEqual 0x10 + 3
			resolver.withLpcOffset(LpcMessageCode.BlockHashes.asByte) mustEqual 0x10 + 4
			resolver.withLpcOffset(LpcMessageCode.GetBlocks.asByte) mustEqual 0x10 + 5
			resolver.withLpcOffset(LpcMessageCode.Blocks.asByte) mustEqual 0x10 + 6
			resolver.withLpcOffset(LpcMessageCode.NewBlock.asByte) mustEqual 0x10 + 7
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashesByNumber.asByte) mustEqual 0x10 + 8

			resolver.withShhOffset(ShhMessageCode.Status.asByte) mustEqual 0x19 + 0
			resolver.withShhOffset(ShhMessageCode.Message.asByte) mustEqual 0x19 + 1
			resolver.withShhOffset(ShhMessageCode.AddFilter.asByte) mustEqual 0x19 + 2
			resolver.withShhOffset(ShhMessageCode.RemoveFilter.asByte) mustEqual 0x19 + 3
			resolver.withShhOffset(ShhMessageCode.PacketCount.asByte) mustEqual 0x19 + 4
		}
	}

	"test (5)" should {
		"be right" in {
			val resolver = new MessageCodesResolver(Seq(new Capability(Capability.SHH, 0), new Capability(Capability.LPC, LpcVersion.SupportedVersions.last.toByte)))

			resolver.withLpcOffset(LpcMessageCode.Status.asByte) mustEqual 0x10 + 0
			resolver.withLpcOffset(LpcMessageCode.NewBlockHashes.asByte) mustEqual 0x10 + 1
			resolver.withLpcOffset(LpcMessageCode.Transactions.asByte) mustEqual 0x10 + 2
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashes.asByte) mustEqual 0x10 + 3
			resolver.withLpcOffset(LpcMessageCode.BlockHashes.asByte) mustEqual 0x10 + 4
			resolver.withLpcOffset(LpcMessageCode.GetBlocks.asByte) mustEqual 0x10 + 5
			resolver.withLpcOffset(LpcMessageCode.Blocks.asByte) mustEqual 0x10 + 6
			resolver.withLpcOffset(LpcMessageCode.NewBlock.asByte) mustEqual 0x10 + 7
			resolver.withLpcOffset(LpcMessageCode.GetBlockHashesByNumber.asByte) mustEqual 0x10 + 8

			resolver.withShhOffset(ShhMessageCode.Status.asByte) mustEqual 0x19 + 0
			resolver.withShhOffset(ShhMessageCode.Message.asByte) mustEqual 0x19 + 1
			resolver.withShhOffset(ShhMessageCode.AddFilter.asByte) mustEqual 0x19 + 2
			resolver.withShhOffset(ShhMessageCode.RemoveFilter.asByte) mustEqual 0x19 + 3
			resolver.withShhOffset(ShhMessageCode.PacketCount.asByte) mustEqual 0x19 + 4
		}
	}

}
