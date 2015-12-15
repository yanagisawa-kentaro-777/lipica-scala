package org.lipicalabs.lipica.core.config

import org.lipicalabs.lipica.core.net.transport.Node

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

	def listenPort: Int = {
		//TODO
		21000
	}

	def projectVersion: String = {
		//TODO
		"0.5.0.0"
	}

	def helloPhrase: String = {
		//TODO
		"audentis fortuna iuuat"
	}

	def maxHashesAsk: Int = {
		//TODO
		10000
	}

	def maxBlocksAsk: Int = {
		//TODO
		100
	}

	def networkId: Int = {
		//TODO
		1
	}

	def isSyncEnabled: Boolean = {
		//TODO
		true
	}

	def activePeers: Seq[Node] = {
		//TODO
		Seq.empty
	}

	def syncPeersCount: Int = {
		//TODO
		8
	}

	def peerConnectionTimeoutMillis: Int = {
		//TODO
		10000
	}

	def peerChannelReadTimeoutSeconds: Int = {
		//TODO
		10
	}

	def peerDiscoveryWorkers: Int = {
		//TODO
		8
	}

	def peerDiscoveryAddresses: Seq[String] = {
		//TODO
		Seq("54.94.239.50:30303", "52.16.188.185:30303")
	}

}

object SystemProperties {
	val CONFIG = new SystemProperties
}