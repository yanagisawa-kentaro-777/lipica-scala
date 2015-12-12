package org.lipicalabs.lipica.core.facade

import org.lipicalabs.lipica.core.net.transport.Node

/**
 * Created by IntelliJ IDEA.
 * 2015/12/02 20:50
 * YANAGISAWA, Kentaro
 */
trait Lipica {
	def connect(node: Node): Unit
}
