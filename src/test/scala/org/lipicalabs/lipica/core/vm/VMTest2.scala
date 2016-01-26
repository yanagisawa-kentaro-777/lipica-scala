package org.lipicalabs.lipica.core.vm

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.kernel.Address
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.program.Program
import org.lipicalabs.lipica.core.vm.program.Program.IllegalOperationException
import org.lipicalabs.lipica.core.vm.program.context.ProgramContextMockImpl
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner
import org.specs2.specification.BeforeExample

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class VMTest2 extends Specification with BeforeExample {
	sequential

	private var context = new ProgramContextMockImpl(null)

	override def before: scala.Any = {
		val ownerAddress = Address.parseHexString("77045E71A7A2C50903D88E564CD72FAB11E82051")
		val msgData = ImmutableBytes.parseHexString("00000000000000000000000000000000000000000000000000000000000000A1" + "00000000000000000000000000000000000000000000000000000000000000B1")

		context = new ProgramContextMockImpl(msgData)
		context.setOwnerAddress(ownerAddress)

		context.repository.createAccount(ownerAddress)
		context.repository.addBalance(ownerAddress, BigInt(1000L))
	}

	"calldatasize (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("36"), context)
			val expected = "0000000000000000000000000000000000000000000000000000000000000040"

			vm.step(program)

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"calldataload (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600035"), context)
			val expected = "00000000000000000000000000000000000000000000000000000000000000A1"

			(0 until 2).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"calldatacopy (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60206000600037"), context)
			val expected = "00000000000000000000000000000000000000000000000000000000000000A1"

			(0 until 4).foreach {
				_ => vm.step(program)
			}

			program.getMemoryContent.toHexString.toUpperCase mustEqual expected
		}
	}

	"address (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("30"), context)
			val expected = "00000000000000000000000077045E71A7A2C50903D88E564CD72FAB11E82051"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"balance (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("3031"), context)
			val expected = "00000000000000000000000000000000000000000000000000000000000003E8"

			(0 until 2).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"origin" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("32"), context)
			val expected = "00000000000000000000000013978AEE95F38490E9769C39B2773ED763D9CD5F"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"caller" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("33"), context)
			val expected = "000000000000000000000000885F93EED577F2FC341EBB9A5C9B2CE4465D96C4"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"callvalue" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("34"), context)
			val expected = "0000000000000000000000000000000000000000000000000DE0B6B3A7640000"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"sha3 (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60016000536001600020"), context)
			val expected = "5FE7F977E71DBA2EA1A68E21057BEEBB9BE2AC30C6410AA38D4F3FBE41DCFFD2"

			(0 until 6).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"blockhash" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600140"), context)
			val expected = "C89EFDAA54C0F20C7ADF612882DF0950F5A951637E0307CDCB4C672F298B8BC6"

			(0 until 2).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"coinbase" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("41"), context)
			val expected = "000000000000000000000000E559DE5527492BCB42EC68D07DF0742A98EC3F1E"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"timestamp" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("42"), context)
			val expected = "000000000000000000000000000000000000000000000000000000005387FE24"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"number" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("43"), context)
			val expected = "0000000000000000000000000000000000000000000000000000000000000021"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"difficulty" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("44"), context)
			val expected = "00000000000000000000000000000000000000000000000000000000003ED290"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"mana price" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("3A"), context)
			val expected = "000000000000000000000000000000000000000000000000000009184E72A000"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}
	//TODO #POC9 問題がある模様。
//	"mana" should {
//		"be right" in {
//			val vm = new VM
//			val program = new Program(ImmutableBytes.parseHexString("5A"), invoke)
//			val expected = "00000000000000000000000000000000000000000000000000000000000F423F"
//
//			(0 until 1).foreach {
//				_ => vm.step(program)
//			}
//
//			program.stackPop.data.toHexString.toUpperCase mustEqual expected
//		}
//	}

	"mana limit" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("45"), context)
			val expected = "00000000000000000000000000000000000000000000000000000000000F4240"

			(0 until 1).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"invalid op" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60012F6002"), context)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"

			try {
				(0 until 2).foreach {
					_ => vm.step(program)
				}
				ko
			} catch {
				case e: IllegalOperationException => ok
				case e: Throwable => ko
			} finally {
				program.isStopped mustEqual true
				program.stackPop.data.toHexString.toUpperCase mustEqual expected
			}
		}
	}
}
