package org.lipicalabs.lipica.core.datasource

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
		getDataSourceFromPool(name, new LevelDbDataSource(name)).asInstanceOf[KeyValueDataSource]
	}

	def hashMapDB(name: String): KeyValueDataSource = {
		getDataSourceFromPool(name, new HashMapDB).asInstanceOf[KeyValueDataSource]
	}

	def closeDataSource(name: String): Unit = {
		pool.remove(name).foreach {
			ds => {
				ds match {
					case hashMapDB: HashMapDB => this.pool.put(name, hashMapDB)
					case _ =>
						ds.synchronized {
							ds.close()
						}
				}
			}
		}
	}

	private def getDataSourceFromPool(name: String, dataSource: DataSource): DataSource = {
		dataSource.setName(name)
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
