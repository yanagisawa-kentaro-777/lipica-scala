package org.lipicalabs.lipica.core.vm.program.listener

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
trait ProgramListenerAware {
	def setTraceListener(listener: ProgramListener)
}
