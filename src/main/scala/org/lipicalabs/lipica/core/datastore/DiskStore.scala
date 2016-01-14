package org.lipicalabs.lipica.core.datastore

import java.io.Closeable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:19
 * YANAGISAWA, Kentaro
 */
trait DiskStore extends Closeable {

	def open(): Unit

	override def close(): Unit

}
