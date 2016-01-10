package org.lipicalabs.lipica.core.db

import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

/**
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class DatabaseImpl(private val keyValueDataSource: KeyValueDataSource) extends Database {

	import DatabaseImpl._

	override def get(key: ImmutableBytes): Option[ImmutableBytes] = this.keyValueDataSource.get(key)

	override def init(): Unit = this.keyValueDataSource.init()

	override def put(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("Putting: [%s] -> [%s]".format(key, value))
		}
		this.keyValueDataSource.put(key, value)
	}

	override def delete(key: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("Deleting: [%s]".format(key))
		}
		this.keyValueDataSource.delete(key)
	}

	override def close(): Unit = this.keyValueDataSource.close()

	def sortedKeys: Seq[ImmutableBytes] = this.keyValueDataSource.keys.toSeq.sorted

	def getDB: KeyValueDataSource = this.keyValueDataSource
}

object DatabaseImpl {
	private val logger = LoggerFactory.getLogger("database")
}