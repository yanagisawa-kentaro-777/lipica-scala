package org.lipicalabs.lipica.core.db.datasource

import org.lipicalabs.lipica.core.utils.ImmutableBytes

trait KeyValueDataSource extends DataSource {

	def get(key: ImmutableBytes): Option[ImmutableBytes]

	def put(key: ImmutableBytes, value: ImmutableBytes): Option[ImmutableBytes]

	def delete(key: ImmutableBytes): Unit

	def keys: Set[ImmutableBytes]

	def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit

}
