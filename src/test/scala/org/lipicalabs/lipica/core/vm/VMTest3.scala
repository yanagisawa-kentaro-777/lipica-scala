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
class VMTest3 extends Specification with BeforeExample {
	sequential

	private var invoke = new ProgramInvokeMockImpl(null)

	override def before: scala.Any = {
		this.invoke = new ProgramInvokeMockImpl(null)
	}

	"test (1)" should {
		"be right" in {
			/**
				#The code will run
				 ------------------

				 a = contract.storage[999]
				 if a > 0:
				 contract.storage[999] = a - 1

				 # call to contract: 77045e71a7a2c50903d88e564cd72fab11e82051
				 send((tx.gas / 10 * 8), 0x77045e71a7a2c50903d88e564cd72fab11e82051, 0)
				 else:
				 stop
			 */

			val key1 = DataWord(999)
			val value1 = DataWord(3)

			// Set contract into Database
			val callerAddr = ImmutableBytes.parseHexString("cd2a3d9f938e13cd947ec05abc7fe734df8dd826")
			val contractAddr = ImmutableBytes.parseHexString("77045e71a7a2c50903d88e564cd72fab11e82051")
			val code = ImmutableBytes.parseHexString("6103e75460005260006000511115630000004c576001600051036103e755600060006000600060007377045e71a7a2c50903d88e564cd72fab11e820516008600a5a0402f1630000004c00565b00")

			val codeKey = code.digest256
			val accountState = new AccountState
			accountState.codeHash = codeKey

			val pi = new ProgramInvokeMockImpl(null)
			pi.setOwnerAddress(contractAddr)
			val repository = pi.getRepository

			try {
				repository.createAccount(callerAddr)
				repository.addBalance(callerAddr, BigInt("100000000000000000000"))

				repository.createAccount(contractAddr)
				repository.saveCode(contractAddr, code)
				repository.addStorageRow(contractAddr, key1, value1)

				// Play the program
				val vm = new VM
				val program = new Program(code, pi)

				try {
					while (!program.isStopped) vm.step(program)
				} catch {
					case e: RuntimeException =>
						program.setRuntimeFailure(e)
				}

				//program.result.manaUsed mustEqual 1000000L
				repository.getBalance(callerAddr).get mustEqual BigInt("100000000000000000000")
				repository.getBalance(contractAddr).get mustEqual BigInt(0)
			} finally {
				repository.close()
			}
		}
	}
}
