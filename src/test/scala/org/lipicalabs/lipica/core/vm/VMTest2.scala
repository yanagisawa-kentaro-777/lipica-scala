package org.lipicalabs.lipica.core.vm

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.base.AccountState
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.program.Program
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeMockImpl
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

	private var invoke = new ProgramInvokeMockImpl(null)

	override def before: scala.Any = {
		val ownerAddress = ImmutableBytes.parseHexString("77045E71A7A2C50903D88E564CD72FAB11E82051")
		val msgData = ImmutableBytes.parseHexString("00000000000000000000000000000000000000000000000000000000000000A1" + "00000000000000000000000000000000000000000000000000000000000000B1")

		invoke = new ProgramInvokeMockImpl(msgData)
		invoke.setOwnerAddress(ownerAddress)

		invoke.getRepository.createAccount(ownerAddress)
		invoke.getRepository.addBalance(ownerAddress, BigInt(1000L))
	}

	"calldatasize (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("36"), invoke)
			val expected = "0000000000000000000000000000000000000000000000000000000000000040"

			vm.step(program)

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"calldataload (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600035"), invoke)
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
			val program = new Program(ImmutableBytes.parseHexString("60206000600037"), invoke)
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
			val program = new Program(ImmutableBytes.parseHexString("30"), invoke)
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
			val program = new Program(ImmutableBytes.parseHexString("3031"), invoke)
			val expected = "00000000000000000000000000000000000000000000000000000000000003E8"

			(0 until 2).foreach {
				_ => vm.step(program)
			}

			program.stackPop.data.toHexString.toUpperCase mustEqual expected
		}
	}

	//TODO origin以下未実装。
}
