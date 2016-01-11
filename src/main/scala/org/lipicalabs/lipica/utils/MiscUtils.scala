package org.lipicalabs.lipica.utils

import java.io.Closeable

/**
 *
 * @since 2016/01/11
 * @author YANAGISAWA, Kentaro
 */
object MiscUtils {

	def isNullOrEmpty(s: String, trim: Boolean): Boolean = {
		if (s eq null) {
			return true
		}
		if (trim) {
			s.trim.isEmpty
		} else {
			s.isEmpty
		}
	}

	def closeIfNotNull(resource: Closeable): Unit = {
		if (resource eq null) {
			return
		}
		try {
			resource.close()
		} catch {
			case any: Throwable => ()
		}
	}

}
