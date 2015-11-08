package org.lipicalabs.lipica.core.datasource

import org.lipicalabs.lipica.core.utils.ImmutableBytes


class HashMapDB extends KeyValueDataSource {

	private[datasource] val storage = new scala.collection.mutable.HashMap[ImmutableBytes, ImmutableBytes]
	private var _clearOnClose = true

	override def delete(arg0: ImmutableBytes): Unit = {
		storage.remove(arg0)
	}

	override def get(arg0: ImmutableBytes): Option[ImmutableBytes] = {
		storage.get(arg0)
	}

	override def put(key: ImmutableBytes, value: ImmutableBytes): Option[ImmutableBytes] = {
		storage.put(key, value)
	}

	def getAddedItems: Int = {
		storage.size
	}

	override def init(): Unit = {
		//
	}

	override def isAlive: Boolean = true

	override def setName(name: String): Unit = {
		//
	}

	override def getName: String = {
		"in-memory"
	}

	override def keys: Set[ImmutableBytes] = {
		this.storage.keySet.toSet
	}

	override def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]) {
		rows.foreach {
			entry => storage.put(entry._1, entry._2)
		}
	}

	def clearOnClose(v: Boolean): HashMapDB = {
		this._clearOnClose = v
		this
	}

	def clearOnClose: Boolean = this._clearOnClose

	override def close(): Unit = {
		if (this.clearOnClose) {
			this.storage.clear()
		}
	}
}