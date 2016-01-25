package org.lipicalabs.lipica.core.datastore.datasource

import java.util.concurrent.ConcurrentHashMap

import com.sleepycat.je.Environment
import org.lipicalabs.lipica.utils.MiscUtils

import scala.collection.JavaConversions

/**
 * Created by IntelliJ IDEA.
 * 2015/11/14 15:52
 * YANAGISAWA, Kentaro
 */
object DataSourcePool {

	private val pool = JavaConversions.mapAsScalaConcurrentMap(new ConcurrentHashMap[String, KeyValueDataSource])

	def levelDbByName(name: String): KeyValueDataSource = {
		val options = LevelDbDataSource.createDefaultOptions
		getDataSourceFromPool(name, new LevelDbDataSource(name, options))
	}

	def bdbByName(name: String, env: Environment): KeyValueDataSource = {
		val config = BdbJeDataSource.createDefaultConfig
		getDataSourceFromPool(name, new BdbJeDataSource(name, env, config))
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

	private def getDataSourceFromPool(name: String, dataSource: KeyValueDataSource): KeyValueDataSource = {
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

	def closeAll(): Unit = {
		this.pool.values.foreach(MiscUtils.closeIfNotNull(_))
	}

}
