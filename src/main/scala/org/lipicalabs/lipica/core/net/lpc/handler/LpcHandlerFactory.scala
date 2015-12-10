package org.lipicalabs.lipica.core.net.lpc.handler

import org.lipicalabs.lipica.core.net.lpc.LpcVersion

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:03
 * YANAGISAWA, Kentaro
 */
object LpcHandlerFactory {

	def create(version: LpcVersion): LpcHandler = new Lpc0

}
