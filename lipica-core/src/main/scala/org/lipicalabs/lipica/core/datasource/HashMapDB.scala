package org.lipicalabs.lipica.core.datasource

import org.lipicalabs.lipica.core.utils.ByteArrayWrapper


class HashMapDB extends KeyValueDataSource {

	private[datasource] val storage = new scala.collection.mutable.HashMap[ByteArrayWrapper, Array[Byte]]

	private def wrap(data: Array[Byte]): ByteArrayWrapper = ByteArrayWrapper(data)

	override def delete(arg0: Array[Byte]): Unit = {
		storage.remove(wrap(arg0))
	}

	override def get(arg0: Array[Byte]): Option[Array[Byte]] = {
		storage.get(wrap(arg0))
	}

	override def put(key: Array[Byte], value: Array[Byte]): Option[Array[Byte]] = {
		storage.put(wrap(key), value)
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

	override def keys: Set[Array[Byte]] = {
		this.storage.keySet.map(_.data).toSet
	}

	override def updateBatch(rows: Map[Array[Byte], Array[Byte]]) {
		rows.foreach {
			entry => storage.put(wrap(entry._1), entry._2)
		}
	}

	override def close(): Unit = {
		this.storage.clear()
	}
}