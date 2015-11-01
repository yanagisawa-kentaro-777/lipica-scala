package org.lipicalabs.lipica.core.config

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class SystemProperties {

	def vmTrace: Boolean = {
		//TODO
		true
	}
}

object SystemProperties {
	val CONFIG = new SystemProperties
}