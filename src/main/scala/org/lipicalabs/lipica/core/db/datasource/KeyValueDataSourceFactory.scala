package org.lipicalabs.lipica.core.db.datasource

/**
 * Created by IntelliJ IDEA.
 * 2016/01/13 16:07
 * YANAGISAWA, Kentaro
 */
trait KeyValueDataSourceFactory {

	def categoryName: String

	def openDataSource(name: String): KeyValueDataSource

	def closeDataSource(name: String): Unit

}
