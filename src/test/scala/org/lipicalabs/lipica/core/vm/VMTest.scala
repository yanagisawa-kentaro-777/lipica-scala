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

}
