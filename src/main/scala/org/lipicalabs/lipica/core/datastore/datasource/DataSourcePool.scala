package org.lipicalabs.lipica.core.datastore.datasource

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConversions

/**
 * Created by IntelliJ IDEA.
 * 2015/11/14 15:52
 * YANAGISAWA, Kentaro
 */
object DataSourcePool {

	private val pool = JavaConversions.mapAsScalaConcurrentMap(new ConcurrentHashMap[String, DataSource])

	def levelDbByName(name: String): KeyValueDataSource = {
		val options = LevelDbDataSource.createDefaultOptions
		getDataSourceFromPool(name, new LevelDbDataSource(name, options)).asInstanceOf[KeyValueDataSource]
	}

	def hashMapDB(name: String): KeyValueDataSource = {
		getDataSourceFromPool(name, new InMemoryDataSource).asInstanceOf[KeyValueDataSource]
	}

	def closeDataSource(name: String): Unit = {
		pool.remove(name).foreach {
			ds => {
				ds.synchronized {
					ds match {
						case hashMapDB: InMemoryDataSource => this.pool.put(name, hashMapDB)
						case _ => ds.close()
					}
				}
			}
		}
	}

	private def getDataSourceFromPool(name: String, dataSource: DataSource): DataSource = {
		dataSource.name = name
		val result =
			this.pool.putIfAbsent(name, dataSource) match {
				case Some(ds) => ds
				case None => dataSource
			}
		result.synchronized {
			if (!result.isAlive) {
				result.init()
			}
		}
		result
	}

}
