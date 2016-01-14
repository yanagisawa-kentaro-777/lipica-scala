package org.lipicalabs.lipica.core.datastore

import org.lipicalabs.lipica.core.datastore.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory


/**
 * KeyValueDataStore を利用して Database インターフェイスを実装するクラスです。
 *
 * @since 2015/11/08
 * @author YANAGISAWA, Kentaro
 */
class DatabaseImpl(val dataSource: KeyValueDataSource) extends Database {

	import DatabaseImpl._

	override def get(key: ImmutableBytes): Option[ImmutableBytes] = this.dataSource.get(key)

	override def init(): Unit = this.dataSource.init()

	override def put(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<DatabaseImpl> Putting: [%s] -> [%s]".format(key, value))
		}
		this.dataSource.put(key, value)
	}

	def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit = {
		this.dataSource.updateBatch(rows)
	}

	override def delete(key: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<DatabaseImpl> Deleting: [%s]".format(key))
		}
		this.dataSource.delete(key)
	}

	override def close(): Unit = this.dataSource.close()

	/**
	 * 多くの DataSource の実装において、このメソッド呼び出しは高コストです。
	 * みだりに呼び出さないよう注意してください。
	 */
	def sortedKeys: Seq[ImmutableBytes] = this.dataSource.keys.toSeq.sorted

}

object DatabaseImpl {
	private val logger = LoggerFactory.getLogger("database")
}