package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.vm.program.Program
import org.slf4j.LoggerFactory

/**
 * Lipca VMを表すクラスです。
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class VM {

	import VM._

	def step(program: Program): Unit = {
		//
	}

}

object VM {
	private val logger = LoggerFactory.getLogger("VM")
	private val dumpLogger = LoggerFactory.getLogger("dump")

	private val _32_ = BigInt(32L)
	private val logString = "{}    Op: [{}]  Mana: [{}] Deep: [{}]  Hint: [{}]"

	private val MAX_MANA = BigInt(Long.MaxValue)

}