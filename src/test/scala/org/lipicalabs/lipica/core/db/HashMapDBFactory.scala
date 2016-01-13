package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.db.datasource.{KeyValueDataSource, HashMapDB, KeyValueDataSourceFactory}

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2016/01/13 16:19
 * YANAGISAWA, Kentaro
 */
class HashMapDBFactory extends KeyValueDataSourceFactory {

	private val map = new mutable.HashMap[String, HashMapDB]

	private def dataSourceName(givenName: String) = "%s/%s".format(this.categoryName, givenName)

	override def categoryName = "test"

	override def openDataSource(name: String): KeyValueDataSource = {
		val dsName = dataSourceName(name)
		this.map.get(dsName) match {
			case Some(db) => db
			case None =>
				val result = new HashMapDB
				result.setName(dsName)
				this.map.put(dsName, result)
				result
		}
	}

	override def closeDataSource(name: String): Unit = ()
}
