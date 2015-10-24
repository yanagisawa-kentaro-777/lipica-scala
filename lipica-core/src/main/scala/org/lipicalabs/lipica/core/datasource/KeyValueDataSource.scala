package org.lipicalabs.lipica.core.datasource

trait KeyValueDataSource extends DataSource {

	def get(key: Array[Byte]): Option[Array[Byte]]

	def put(key: Array[Byte], value: Array[Byte]): Option[Array[Byte]]

	def delete(key: Array[Byte]): Unit

	def keys: Set[Array[Byte]]

	def updateBatch(rows: Map[Array[Byte], Array[Byte]]): Unit

}
