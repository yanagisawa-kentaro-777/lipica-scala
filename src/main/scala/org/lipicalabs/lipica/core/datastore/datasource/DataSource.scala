package org.lipicalabs.lipica.core.datastore.datasource

import java.io.Closeable

/**
 * データの永続化を行う機構が実装すべき trait です。
 */
trait DataSource extends Closeable {

	def name: String

	def name_=(value: String): Unit

	def init(): Unit

	def isAlive: Boolean

	override def close(): Unit

}
