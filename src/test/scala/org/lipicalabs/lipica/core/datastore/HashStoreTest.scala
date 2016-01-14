package org.lipicalabs.lipica.core.datastore

import java.util.Random

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.datastore.datasource.HashMapDB
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/11/15
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class HashStoreTest extends Specification {
	sequential

	private val hashes = new ArrayBuffer[ImmutableBytes]
	private var hashStore: HashStore = null
	//private var testDBName: String = ""

	private def init(): Unit = {
		val rng = new Random
		for (i <- 0 until 50) {
			val hash = new Array[Byte](32)
			rng.nextBytes(hash)
			this.hashes.append(ImmutableBytes(hash))
		}

		val r = BigInt(32, new Random)
		//this.testDBName = "./work/database/test_db_" + r
		//SystemProperties.CONFIG.databaseDir = this.testDBName
		NodeProperties.CONFIG.databaseReset = false
		this.hashStore = new HashStoreImpl(new HashMapDB)
		this.hashStore.open()
	}

	private def cleanUp(): Unit = {
		try {
			Thread.sleep(100L)
			this.hashStore.clear()
			this.hashStore.close()
			this.hashes.clear()
			Thread.sleep(100L)
			//FileUtils.forceDelete(new java.io.File(this.testDBName))
		} catch {
			case e: Exception =>
				e.printStackTrace()
		}
	}

	"test (1)" should {
		"be right" in {
			try {
				init()

				this.hashes.foreach(this.hashStore.add)
				this.hashStore.close()
				this.hashStore.open()

				this.hashes.foreach {
					each => this.hashStore.poll.get mustEqual each
				}
				this.hashStore.isEmpty mustEqual true
				this.hashStore.peek.isEmpty mustEqual true
				this.hashStore.poll.isEmpty mustEqual true

				for (i <- 0 until 10) {
					this.hashStore.add(this.hashes(i))
				}
				for (i <- 10 until 20) {
					this.hashStore.addFirst(this.hashes(i))
				}
				for (i <- 19 to 10 by -1) {
					hashStore.poll.get mustEqual hashes(i)
				}
				ok
			} finally {
				cleanUp()
			}
		}
	}

	"test (1-2)" should {
		"be right" in {
			try {
				init()
				this.hashStore.isEmpty mustEqual true
				this.hashStore.addBatch(this.hashes)
				this.hashStore.size mustEqual 50
				this.hashStore.size mustEqual this.hashes.size
				this.hashStore.nonEmpty mustEqual true

				val polled = this.hashStore.pollBatch(100)
				polled.size mustEqual this.hashes.size
				this.hashStore.isEmpty mustEqual true
			} finally {
				cleanUp()
			}
		}
	}

	//TODO test(2) - test(3) をスキップ。

}
