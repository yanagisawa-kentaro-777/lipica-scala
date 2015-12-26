package org.lipicalabs.lipica.core.config

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes

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

	def listenAddress: String = {
		//TODO
		"118.238.252.169"
	}

	def listenPort: Int = {
		//TODO
		30303
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

	def peerDiscoveryEnabled: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryPersist: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryWorkers: Int = {
		//TODO
		8
	}

	def peerDiscoveryAddresses: Seq[String] = {
		//TODO
		Seq(
			"enode://de471bccee3d042261d52e9bff31458daecc406142b401d4cd848f677479f73104b9fdeb090af9583d3391b7f10cb2ba9e26865dd5fca4fcdc0fb1e3b723c786@54.94.239.50:30303",
			"enode://a979fb575495b8d6db44f750317d0f4622bf4c2aa3365d6af7c284339968eef29b69ad0dce72a4d8db5ebb4968de0e3bec910127f134779fbcb0cb6d3331163c@52.16.188.185:30303",
			"enode://1118980bf48b0a3640bdba04e0fe78b1add18e1cd99bf22d53daac1fd9972ad650df52176e7c7d89d1114cfef2bc23a2959aa54998a46afcf7d91809f0855082@52.74.57.123:30303"
		)
	}

	def myKey: ECKey = {
		//TODO
		val hex = "a43d867f16238b897428705cec855b0c5b0ddf3319c1b18f7a00915db83155d9"
		ECKey.fromPrivate(Hex.decodeHex(hex.toCharArray)).decompress
	}

	def coinbaseSecret: String = {
		//TODO
		"secret"
	}

	def nodeId: ImmutableBytes = ImmutableBytes(myKey.getNodeId)

	def isPublicHomeNode: Boolean = {
		//TODO
		true
	}

	def peerDiscoveryTouchSeconds: Int = {
		//TODO
		600
	}

	def peerDiscoveryTouchMaxNodes: Int = {
		//TODO
		100
	}

	def blocksFile: String = {
		//TODO
		""
	}

}

object SystemProperties {
	val CONFIG = new SystemProperties
}