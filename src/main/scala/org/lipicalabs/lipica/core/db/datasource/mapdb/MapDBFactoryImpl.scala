package org.lipicalabs.lipica.core.db.datasource.mapdb

import org.lipicalabs.lipica.core.config.SystemProperties
import org.mapdb.{DBMaker, DB}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/26 12:26
 * YANAGISAWA, Kentaro
 */
class MapDBFactoryImpl extends MapDBFactory {

	override def createDataSource = new MapDBDataSource

	override def createDB(name: String) = MapDBFactoryImpl.createDB(name, transactional = false)

	override def createTransactionalDB(name: String) = MapDBFactoryImpl.createDB(name, transactional = true)

}

object MapDBFactoryImpl {
	private[mapdb] def createDB(name: String, transactional: Boolean): DB = {
		val dbFile = new java.io.File(SystemProperties.CONFIG.databaseDir + "/" + name)
		if (!dbFile.getParentFile.exists) {
			dbFile.getParentFile.mkdirs()
		}

		var dbMaker = DBMaker.fileDB(dbFile).closeOnJvmShutdown()
		if (!transactional) {
			dbMaker = dbMaker.transactionDisable()
		}
		dbMaker.make()
	}
}