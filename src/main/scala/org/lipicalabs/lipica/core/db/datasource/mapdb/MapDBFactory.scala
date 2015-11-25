package org.lipicalabs.lipica.core.db.datasource.mapdb

import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.mapdb.DB

/**
 * Created by IntelliJ IDEA.
 * 2015/11/25 18:53
 * YANAGISAWA, Kentaro
 */
trait MapDBFactory {

	def createDataSource: KeyValueDataSource

	def createDB(name: String): DB

	def createTransactionalDB(name: String): DB

}
