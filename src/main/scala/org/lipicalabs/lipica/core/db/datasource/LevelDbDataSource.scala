package org.lipicalabs.lipica.core.db.datasource

import java.nio.file.{Files, Path, Paths}

import org.iq80.leveldb
import org.iq80.leveldb.{CompressionType, DB, Options}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18 12:07
 * YANAGISAWA, Kentaro
 */
class LevelDbDataSource(_name: String) extends KeyValueDataSource {
	import LevelDbDataSource._

	private var name: String = _name
	private var db: DB = null
	private var alive = false

	override def init(): Unit = {
		if (this.alive) {
			return
		}
		try {
			val options = new Options
			options.createIfMissing(true)
			options.compressionType(CompressionType.NONE)
			options.blockSize(10 * 1024 * 1024)
			options.writeBufferSize(10 * 1024 * 1024)
			options.cacheSize(0)
			options.paranoidChecks(true)
			options.verifyChecksums(true)

			if (logger.isDebugEnabled) {
				logger.debug("<LevelDbDataSource> Opening database: %s".format(this.name))
			}
			val dbPath = Paths.get(SystemProperties.CONFIG.databaseDir, this.name)
			Files.createDirectories(dbPath.getParent)

			if (logger.isDebugEnabled) {
				logger.debug("<LevelDbDataSource> Initializing database: %s at %s".format(this.name, dbPath))
			}
			this.db = org.fusesource.leveldbjni.JniDBFactory.factory.open(dbPath.toFile, options)
			this.alive = true
		} catch {
			case e: Exception =>
				logger.warn("<LevelDbDataSource>", e)
				throw new RuntimeException(e)
		}
	}

	def destroyDB(path: Path): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<LevelDbDataSource> Destroying database at %s".format(path))
		}
		try {
			org.fusesource.leveldbjni.JniDBFactory.factory.destroy(path.toFile, new leveldb.Options)
		} catch {
			case e: Exception =>
				logger.warn("<LevelDbDataSource>", e)
		}
	}

	override def isAlive: Boolean = this.alive

	override def close(): Unit = {
		if (!isAlive) {
			return
		}
		if (logger.isDebugEnabled) {
			logger.debug("<LevelDbDataSource> Closing database: %s".format(this.name))
		}
		try {
			this.db.close()
			this.alive = false
		} catch {
			case e: Exception =>
				logger.warn("<LevelDbDataSource>", e)
		}
	}

	override def setName(v: String): Unit = this.name = v

	override def getName: String = this.name

	override def get(key: ImmutableBytes): Option[ImmutableBytes] = {
		val result = this.db.get(key.toByteArray)
		if (result eq null) {
			None
		} else {
			Some(ImmutableBytes(result))
		}
	}

	override def put(key: ImmutableBytes, value: ImmutableBytes): Option[ImmutableBytes] = {
		this.db.put(key.toByteArray, value.toByteArray)
		Option(value)
	}

	override def delete(key: ImmutableBytes): Unit = {
		this.db.delete(key.toByteArray)
	}

	override def keys: Set[ImmutableBytes] = {
		val result = new mutable.HashSet[ImmutableBytes]
		val it = db.iterator()
		it.seekToFirst()
		while (it.hasNext) {
			result.add(ImmutableBytes(it.peekNext().getKey))
			it.next()
		}
		result.toSet
	}

	private def updateBatchInternal(rows: Map[ImmutableBytes, ImmutableBytes]): Unit = {
		val batch = this.db.createWriteBatch()
		try {
			for (entry <- rows) {
				batch.put(entry._1.toByteArray, entry._2.toByteArray)
			}
			this.db.write(batch)
		} finally {
			batch.close()
		}
	}

	override def updateBatch(rows: Map[ImmutableBytes, ImmutableBytes]): Unit = {
		try {
			updateBatchInternal(rows)
		} catch {
			case e: Exception =>
				//もう一度。
				Thread.sleep(50L)
				try {
					updateBatch(rows)
				} catch {
					case e1: Exception =>
						logger.warn("<LevelDbDataSource>", e1)
						throw new RuntimeException(e1)
				}
		}
	}
}

object LevelDbDataSource {
	private val logger = LoggerFactory.getLogger(getClass)
}