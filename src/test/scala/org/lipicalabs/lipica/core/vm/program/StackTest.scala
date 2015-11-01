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
class StackTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val stack = new Stack
			stack.size mustEqual 0
			stack.isEmpty mustEqual true
			stack.nonEmpty mustEqual false

			stack.push(DataWord(0L))

			stack.isEmpty mustEqual false
			stack.nonEmpty mustEqual true
			stack.size mustEqual 1

			stack.push(DataWord(1L))

			stack.isEmpty mustEqual false
			stack.nonEmpty mustEqual true
			stack.size mustEqual 2

			stack.pop mustEqual DataWord(1L)

			stack.isEmpty mustEqual false
			stack.nonEmpty mustEqual true
			stack.size mustEqual 1

			stack.pop mustEqual DataWord(0L)

			stack.size mustEqual 0
			stack.isEmpty mustEqual true
			stack.nonEmpty mustEqual false

			try {
				stack.pop
				ko
			} catch {
				case any: Throwable => ok
			}
		}
	}

	"test (2)" should {
		"be right" in {
			val stack = new Stack
			stack.push(DataWord(0L))
			stack.push(DataWord(1L))

			stack.swap(0, 1).isRight mustEqual true

			stack.pop mustEqual DataWord(0L)
			stack.pop mustEqual DataWord(1L)

			stack.swap(0, 2).isRight mustEqual false
			stack.swap(-1, 1).isRight mustEqual false

			stack.push(DataWord(0L))
			stack.push(DataWord(1L))

			stack.swap(0, 1).isRight mustEqual true
			stack.swap(1, 0).isRight mustEqual true

			stack.asIterable.size mustEqual 2

			stack.pop mustEqual DataWord(1L)
			stack.pop mustEqual DataWord(0L)

			stack.asIterable.size mustEqual 0
		}
	}

}
