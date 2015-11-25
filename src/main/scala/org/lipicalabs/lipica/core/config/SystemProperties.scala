package org.lipicalabs.lipica.core.config

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
class SystemProperties {

	def vmTrace: Boolean = {
		//TODO
		true
	}

	def vmTraceInitStorageLimit: Int = {
		//TODO
		1000000
	}

	def isStorageDictionaryEnabled: Boolean = {
		//TODO
		false
	}

	def dumpBlock: Long = {
		//TODO
		-1L
	}

	def detailsInMemoryStorageLimit: Int = {
		//TODO
		1000
	}

	def isFrontier: Boolean = {
		//TODO
		false
	}

	def databaseDir: String = {
		//TODO
		new java.io.File("./work/database/").getAbsolutePath
	}

	def genesisInfo: String = {
		//TODO
		"genesis1.json"
	}

	def databaseReset: Boolean = {
		//TODO
		true
	}

}

object SystemProperties {
	val CONFIG = new SystemProperties
}