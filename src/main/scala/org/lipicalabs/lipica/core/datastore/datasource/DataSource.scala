package org.lipicalabs.lipica.core.datastore.datasource

import java.io.Closeable

trait DataSource extends Closeable {

	 def setName(value: String): Unit

	 def getName: String

	 def init(): Unit

	 def isAlive: Boolean

	def deleteAll(): Unit

	 override def close(): Unit

 }
