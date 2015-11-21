package org.lipicalabs.lipica.core.db.datasource

trait DataSource {

	 def setName(value: String): Unit

	 def getName: String

	 def init(): Unit

	 def isAlive: Boolean

	 def close(): Unit

 }
