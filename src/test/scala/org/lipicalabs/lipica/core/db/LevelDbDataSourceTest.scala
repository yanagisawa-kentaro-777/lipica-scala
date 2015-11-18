package org.lipicalabs.lipica.core.db

import java.nio.file.{Paths, Files}
import java.util.Random

import org.apache.commons.io.FileUtils
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.datasource.LevelDbDataSource
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/11/18
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class LevelDbDataSourceTest extends Specification {
	sequential


	"io (1)" should {
		"be right" in {
			val dataSource = new LevelDbDataSource("test")
			try {
				FileUtils.forceDelete(Paths.get(SystemProperties.CONFIG.databaseDir).toFile)
				dataSource.init()

				val batchSize = 100
				val batch = createBatch(batchSize)

				dataSource.updateBatch(batch)

				for (entry <- batch) {
					dataSource.get(entry._1).get mustEqual entry._2
				}
				dataSource.keys.size mustEqual batchSize
			} finally {
				dataSource.close()
			}
		}
	}

	"io (2)" should {
		"be right" in {
			val dataSource = new LevelDbDataSource("test")
			try {
				FileUtils.forceDelete(Paths.get(SystemProperties.CONFIG.databaseDir).toFile)
				dataSource.init()

				val r = new Random
				val key = generateRandomBytes(r, 32)
				val value = generateRandomBytes(r, 32)

				dataSource.put(key, value)

				dataSource.keys.size mustEqual 1
				dataSource.get(key).get mustEqual value

				dataSource.delete(key)

				dataSource.keys.size mustEqual 0
				dataSource.get(key).isEmpty mustEqual true
			} finally {
				dataSource.close()
			}
		}
	}

	private def createBatch(size: Int): Map[ImmutableBytes, ImmutableBytes] = {
		val random = new Random()
		(0 until size).map {
			_ => {
				(generateRandomBytes(random, 32), generateRandomBytes(random, 32))
			}
		}.toMap
	}

	private def generateRandomBytes(r: Random, size: Int): ImmutableBytes = {
		val result = new Array[Byte](size)
		r.nextBytes(result)
		ImmutableBytes(result)
	}

}
