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

	private var _databaseDir: String = "./work/database/"
	def databaseDir_=(v: String): Unit = {
		this._databaseDir = v
	}
	def databaseDir: String = {
		//TODO
		new java.io.File(this._databaseDir).getAbsolutePath
	}

	def genesisInfo: String = {
		//TODO
		"genesis1.json"
	}

	private var _databaseReset: Boolean = false
	def databaseReset_=(v: Boolean): Unit = {
		this._databaseReset = v
	}
	def databaseReset: Boolean = {
		//TODO
		this._databaseReset
	}

	//ブロック内のコード実行を行わない場合、blockchainOnlyとする。
	private var _blockchainOnly = false
	def blockchainOnly_=(v: Boolean): Unit = this._blockchainOnly = v
	def blockchainOnly: Boolean = {
		//TODO
		this._blockchainOnly
	}

	//TODO
	private var _recordBlocks = false
	def recordBlocks_=(v: Boolean): Unit = this._recordBlocks = v
	def recordBlocks: Boolean = this._recordBlocks

	def dumpDir: String = {
		//TODO
		"dump"
	}

	def cacheFlushMemory: Double = 0d
	def cacheFlushBlocks: Int = 0

	def txOutdatedThreshold: Int = 3

}

object SystemProperties {
	val CONFIG = new SystemProperties
}