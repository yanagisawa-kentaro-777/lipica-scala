package org.lipicalabs.lipica.core.datastore.datasource

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.{AtomicReference, AtomicBoolean}

import org.lipicalabs.lipica.core.utils.ImmutableBytes

class InMemoryDataSource extends KeyValueDataSource {
	import scala.collection.JavaConversions._

	private[datasource] val storage = mapAsScalaConcurrentMap(new ConcurrentHashMap[ImmutableBytes, ImmutableBytes])

	/**
	 * テスト用に、close しても消さない設定というのがある。
	 */
	private val clearOnCloseRef = new AtomicBoolean(false)
	def clearOnClose: Boolean = this.clearOnCloseRef.get
	def clearOnClose_=(v: Boolean): Unit = this.clearOnCloseRef.set(v)

	override def delete(arg0: ImmutableBytes): Unit = {
		storage.remove(arg0)
	}

	override def get(arg0: ImmutableBytes): Option[ImmutableBytes] = {
		storage.get(arg0)
	}

	override def put(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		storage.put(key, value)
	}

	def getAddedItems: Int = {
		storage.size
	}

	override def init(): Unit = {
		//
	}

	override def isAlive: Boolean = true

	private val nameRef: AtomicReference[String] = new AtomicReference[String]("in-memory")
	override def name: String = this.nameRef.get
	override def name_=(v: String): Unit = this.nameRef.set(v)

	override def deleteAll(): Unit = {
		this.storage.clear()
	}

	override def keys: Set[ImmutableBytes] = {
		this.storage.keySet.toSet
	}

	override def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]) {
		rows.foreach {
			entry => storage.put(entry._1, entry._2)
		}
	}

	override def close(): Unit = {
		if (this.clearOnClose) {
			this.storage.clear()
		}
	}
}