package org.lipicalabs.lipica.core.vm

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.program.Program
import org.lipicalabs.lipica.core.vm.program.invoke.ProgramInvokeMockImpl
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class VMTest extends Specification {
	sequential

	private val invoke = new ProgramInvokeMockImpl(null)

	"test push1" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60A0"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000000000000000000000A0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push2" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("61A0B0"), invoke, null)
			val expected = "000000000000000000000000000000000000000000000000000000000000A0B0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push3" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("62A0B0C0"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000A0B0C0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push4" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("63A0B0C0D0"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000000000000000A0B0C0D0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push5" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("64A0B0C0D0E0"), invoke, null)
			val expected = "000000000000000000000000000000000000000000000000000000A0B0C0D0E0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push6" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("65A0B0C0D0E0F0"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000A0B0C0D0E0F0"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push7" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("66A0B0C0D0E0F0A1"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000000000A0B0C0D0E0F0A1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push8" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("67A0B0C0D0E0F0A1B1"), invoke, null)
			val expected = "000000000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push9" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("68A0B0C0D0E0F0A1B1C1"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push10" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("69A0B0C0D0E0F0A1B1C1D1"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push11" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6AA0B0C0D0E0F0A1B1C1D1E1"), invoke, null)
			val expected = "000000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push12" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6BA0B0C0D0E0F0A1B1C1D1E1F1"), invoke, null)
			val expected = "0000000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push13" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6CA0B0C0D0E0F0A1B1C1D1E1F1A2"), invoke, null)
			val expected = "00000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push14" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6DA0B0C0D0E0F0A1B1C1D1E1F1A2B2"), invoke, null)
			val expected = "000000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push15" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6EA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2"), invoke, null)
			val expected = "0000000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push16" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6FA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2"), invoke, null)
			val expected = "00000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push17" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("70A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2"), invoke, null)
			val expected = "000000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push18" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("71A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2"), invoke, null)
			val expected = "0000000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push19" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("72A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3"), invoke, null)
			val expected = "00000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push20" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("73A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3"), invoke, null)
			val expected = "000000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push21" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("74A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3"), invoke, null)
			val expected = "0000000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push22" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("75A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3"), invoke, null)
			val expected = "00000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push23" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("76A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3"), invoke, null)
			val expected = "000000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push24" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("77A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3"), invoke, null)
			val expected = "0000000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push25" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("78A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4"), invoke, null)
			val expected = "00000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push26" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("79A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4"), invoke, null)
			val expected = "000000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push27" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7AA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4"), invoke, null)
			val expected = "0000000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push28" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7BA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4"), invoke, null)
			val expected = "00000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push29" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7CA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4"), invoke, null)
			val expected = "000000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push30" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7DA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4"), invoke, null)
			val expected = "0000A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push31" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7EA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1"), invoke, null)
			val expected = "00A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push32" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7FA0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1"), invoke, null)
			val expected = "A0B0C0D0E0F0A1B1C1D1E1F1A2B2C2D2E2F2A3B3C3D3E3F3A4B4C4D4E4F4A1B1"
			vm.step(program)
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test push not enough data (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("61AA"), invoke, null)
			val expected = "000000000000000000000000000000000000000000000000000000000000AA00"
			vm.step(program)
			program.isStopped mustEqual true
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}
	"test push not enough data (2)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("7fAABB"), invoke, null)
			val expected = "AABB000000000000000000000000000000000000000000000000000000000000"
			vm.step(program)
			program.isStopped mustEqual true
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test and (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600A600A16"), invoke, null)
			val expected = "000000000000000000000000000000000000000000000000000000000000000A"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test or (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60F0600F17"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000000000000000000000FF"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test xor (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60FF60FF18"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000000"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test byte (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("65AABBCCDDEEFF601E1A"), invoke, null)
			val expected = "00000000000000000000000000000000000000000000000000000000000000EE"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test is zero (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600015"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 2).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test eq (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("602A602A14"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test gt (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6001600211"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test sgt (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6001600213"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test lt (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6001600210"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000000"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test slt (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6001600212"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000000"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test bnot (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600119"), invoke, null)
			val expected = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFE"
			(0 until 2).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test pop (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("61000060016200000250"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 4).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test dupN (1)" should {
		"be right" in {
			(1 to 16).foreach {
				testDupN
			}
			ok
		}
	}

	private def testDupN(n: Int): Unit = {
		val vm = new VM
		val operation = (OpCode.Dup1.opcode + n - 1).toByte
		val programCode = new StringBuilder
		(0 until n).foreach {
			i => {
				programCode.append("60" + (12 + i))
			}
		}
		val code = ImmutableBytes.parseHexString(programCode.toString()) :+ operation
		val program = new Program(code, invoke, null)
		val expected = "0000000000000000000000000000000000000000000000000000000000000012"
		val expectedLen = n + 1
		(0 until expectedLen).foreach {
			_ => vm.step(program)
		}

		program.stack.size mustEqual expectedLen
		program.stack.pop.data.toHexString.toUpperCase mustEqual expected
		(0 until (expectedLen - 2)).foreach {
			_ => program.stack.pop.data.toHexString.toUpperCase mustNotEqual expected
		}
		program.stack.pop.data.toHexString.toUpperCase mustEqual expected
	}

	//TODO Swap 以下、間を大きく飛ばしている。

	"test add (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6002600201"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000004"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test add mod (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60026002600308"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000001"
			(0 until 4).foreach {
				_ => vm.step(program)
			}
			program.isStopped mustEqual true
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test add mod (2)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6110006002611002086000"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000004"
			(0 until 4).foreach {
				_ => vm.step(program)
			}
			program.isStopped mustEqual false
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test mul (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6003600202"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000006"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test mul mod (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60036002600409"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000002"
			(0 until 4).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test div (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6002600404"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000002"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test sdiv (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6103E87FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFC1805"), invoke, null)
			val expected = "FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test sub (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("6004600603"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000002"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test msize (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("59"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000000"
			(0 until 1).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
		}
	}

	"test stop" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("60206030601060306011602300"), invoke, null)
			val expectedSteps = 7
			var count = 0
			while (!program.isStopped) {
				vm.step(program)
				count += 1
			}
			count mustEqual expectedSteps
		}
	}

	"test exp (1)" should {
		"be right" in {
			val vm = new VM
			val program = new Program(ImmutableBytes.parseHexString("600360020a"), invoke, null)
			val expected = "0000000000000000000000000000000000000000000000000000000000000008"
			(0 until 3).foreach {
				_ => vm.step(program)
			}
			program.stack.peek.data.toHexString.toUpperCase mustEqual expected
			program.result.manaUsed mustEqual 26
		}
	}

	//TODO RETURN 以下未実装。

}
