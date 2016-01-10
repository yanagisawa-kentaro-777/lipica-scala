package org.lipicalabs.lipica.core.kernel.genesis


/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:26
 * YANAGISAWA, Kentaro
 */
class GenesisJson {
	private[genesis] var mixhash: String = ""
	private[genesis] var coinbase: String = ""
	private[genesis] var timestamp: String = ""
	private[genesis] var parentHash: String = ""
	private[genesis] var extraData: String = ""
	private[genesis] var manaLimit: String = ""
	private[genesis] var nonce: String = ""
	private[genesis] var difficulty: String = ""
	private[genesis] var alloc: java.util.Map[String, AllocatedAccount] = new java.util.TreeMap[String, AllocatedAccount]


	def getMixhash: String = mixhash

	def setMixhash(mixhash: String) {
		this.mixhash = mixhash
	}

	def getCoinbase: String = coinbase

	def setCoinbase(coinbase: String) {
		this.coinbase = coinbase
	}

	def getTimestamp: String = timestamp

	def setTimestamp(timestamp: String) {
		this.timestamp = timestamp
	}

	def getParentHash: String = parentHash

	def setParentHash(parentHash: String) {
		this.parentHash = parentHash
	}

	def getExtraData: String = extraData

	def setExtraData(extraData: String) {
		this.extraData = extraData
	}

	def getManaLimit: String = manaLimit

	def setManaLimit(manaLimit: String) {
		this.manaLimit = manaLimit
	}

	def getNonce: String = nonce

	def setNonce(nonce: String) {
		this.nonce = nonce
	}

	def getDifficulty: String = difficulty

	def setDifficulty(difficulty: String) {
		this.difficulty = difficulty
	}

	def getAlloc: java.util.Map[String, AllocatedAccount] = alloc

	def setAlloc(alloc: java.util.Map[String, AllocatedAccount]) {
		this.alloc = alloc
	}
}
