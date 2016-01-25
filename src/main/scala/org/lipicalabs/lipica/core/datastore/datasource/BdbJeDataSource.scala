package org.lipicalabs.lipica.core.datastore.datasource

import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import com.sleepycat.je._
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.utils.MiscUtils

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2016/01/25 19:08
 * YANAGISAWA, Kentaro
 */
class BdbJeDataSource(_name: String, private val env: Environment, private val dbConfig: DatabaseConfig) extends KeyValueDataSource {

	private val isAliveRef = new AtomicBoolean(false)
	override def isAlive: Boolean = this.isAliveRef.get

	private val nameRef: AtomicReference[String] = new AtomicReference[String](_name)
	override def name_=(v: String): Unit = this.nameRef.set(v)
	override def name: String = this.nameRef.get

	private val databaseRef: AtomicReference[Database] = new AtomicReference[Database](null)
	private def database: Database = this.databaseRef.get

	override def init(): Unit = {
		if (this.isAlive) {
			return
		}
		val db = env.openDatabase(null, this.name, dbConfig)
		this.databaseRef.set(db)
		this.isAliveRef.set(true)
	}

	override def close(): Unit = {
		if (!isAlive) {
			return
		}
		MiscUtils.closeIfNotNull(this.database)
		this.isAliveRef.set(false)
	}

	/**
	 * 指定されたキーに対応する値を返します。
	 *
	 * @param key キー。
	 * @return 対応する値があればその値、なければ None。
	 */
	override def get(key: ImmutableBytes): Option[ImmutableBytes] = {
		val keyEntry = new DatabaseEntry(key.toByteArray)
		val valueEntry = new DatabaseEntry
		if (this.database.get(null, keyEntry, valueEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
			Option(ImmutableBytes(valueEntry.getData))
		} else {
			None
		}
	}

	/**
	 * 指定されたキーに指定された値を結びつけて保存します。
	 *
	 * @param key キー。
	 * @param value 値。
	 * @return 以前に結び付けられていた値があれば、その値。
	 */
	override def put(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		val keyEntry = new DatabaseEntry(key.toByteArray)
		val valueEntry = new DatabaseEntry(value.toByteArray)
		this.database.put(null, keyEntry, valueEntry)
	}

	/**
	 * 指定されたキーおよびそれに対応する値を削除します。
	 *
	 * @param key キー。
	 */
	override def delete(key: ImmutableBytes): Unit = {
		val keyEntry = new DatabaseEntry(key.toByteArray)
		this.database.delete(null, keyEntry)
	}

	/**
	 * 指定されたキーおよび値の連想配列の要素を一括して、
	 * 可能ならば効率よく、登録します。
	 *
	 * @param rows キーおよび値の連想配列。
	 */
	override def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit = {
		for (row <- rows) {
			this.put(row._1, row._2)
		}
	}

	/**
	 * すべてのキーの集合を返します。
	 *
	 * 多くの実装において、この操作は非常に高負荷であると予想されますので、
	 * みだりに呼び出さないようにしてください。
	 */
	override def keys: Set[ImmutableBytes] = {
		val cursor = this.database.openCursor(null, CursorConfig.DEFAULT)
		try {
			val buffer = new ArrayBuffer[ImmutableBytes]
			val keyEntry = new DatabaseEntry
			val valueEntry = new DatabaseEntry
			while (cursor.getNext(keyEntry, valueEntry, LockMode.DEFAULT) == OperationStatus.SUCCESS) {
				buffer.append(ImmutableBytes(keyEntry.getData))
			}
			buffer.toSet
		} finally {
			MiscUtils.closeIfNotNull(cursor)
		}
	}

	/**
	 * すべての要素を削除します。
	 *
	 * 多くの実装において、この操作は非常に高負荷であると予想されますので、
	 * みだりに呼び出さないようにしてください。
	 */
	override def deleteAll(): Unit = {
		for (key <- keys) {
			this.delete(key)
		}
	}

}

object BdbJeDataSource {

	def createDefaultEnvironment(envHome: Path): Environment = {
		val home = envHome.toFile
		if (!home.exists) {
			home.mkdirs()
		}
		val envConfig = new EnvironmentConfig
		envConfig.setTransactional(false)
		envConfig.setDurability(Durability.COMMIT_WRITE_NO_SYNC)
		envConfig.setAllowCreate(true)
		envConfig.setLockTimeout(30000, TimeUnit.MILLISECONDS)
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_MIN_UTILIZATION, 50.toString)
		envConfig.setConfigParam(EnvironmentConfig.CLEANER_MIN_FILE_UTILIZATION, 5.toString)
		envConfig.setConfigParam(EnvironmentConfig.LOG_CHECKSUM_READ, false.toString)
		envConfig.setConfigParam(EnvironmentConfig.LOG_FILE_MAX, "1000000000")
		envConfig.setCachePercent(50)

		new Environment(home, envConfig)
	}

	def createDefaultConfig: DatabaseConfig = {
		val result = new DatabaseConfig
		result.setTransactional(false)
		result.setDeferredWrite(false)
		result.setAllowCreate(true)
		result.setSortedDuplicates(false)
		result
	}


}
