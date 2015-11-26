package org.lipicalabs.lipica.core.db.datasource.mapdb

import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.mapdb.DB

import scala.collection.JavaConversions

/**
 * Created by IntelliJ IDEA.
 * 2015/11/26 12:02
 * YANAGISAWA, Kentaro
 */
class MapDBDataSource extends KeyValueDataSource {
	import MapDBDataSource._

	private var db: DB = null
	private var map: java.util.Map[ImmutableBytes, ImmutableBytes] = null
	private var _name: String = ""
	private var alive: Boolean = false

	override def init(): Unit = {
		this.db = MapDBFactoryImpl.createDB(this.getName, transactional = false)
		this.map = this.db.hashMapCreate(this._name).keySerializer(Serializers.ImmutableBytes).valueSerializer(Serializers.ImmutableBytes).makeOrGet()
		this.alive = true
	}

	override def isAlive = this.alive
	override def setName(v: String): Unit = this._name = v
	override def getName: String = this._name
	override def get(key: ImmutableBytes): Option[ImmutableBytes] = Option(this.map.get(key))
	override def put(key: ImmutableBytes, value: ImmutableBytes): Option[ImmutableBytes] = {
		try {
			Option(this.map.put(key, value))
		} finally {
			this.db.commit()
		}
	}

	override def delete(key: ImmutableBytes): Unit = {
		try {
			this.map.remove(key)
		} finally {
			this.db.commit()
		}
	}

	override def keys: Set[ImmutableBytes] = JavaConversions.asScalaSet(this.map.keySet).toSet

	override def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit = {
		try {
			var savedSize = 0
			for (row <- rows) {
				val (key, value) = row
				savedSize += value.length

				this.map.put(key, value)
				if (BatchSize < savedSize) {
					this.db.commit()
					savedSize = 0
				}
			}
		} finally {
			this.db.commit()
		}
	}

	override def close(): Unit = {
		this.db.close()
		this.alive = false
	}

}

object MapDBDataSource {
	private val BatchSize = 1024 * 1000 * 10
}