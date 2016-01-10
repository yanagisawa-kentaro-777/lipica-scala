package org.lipicalabs.lipica.core.sync

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 19:57
 * YANAGISAWA, Kentaro
 */
class SyncStatistics {

	private var updatedAt: Long = 0L
	private var _blocksCount: Long = 0L
	def blocksCount: Long = this._blocksCount

	private var _hashesCount: Long = 0L
	def hashesCount: Long = this._hashesCount

	private var _emptyResponsesCount: Int = 0
	def emptyResponsesCount: Int = this._emptyResponsesCount

	def reset(): Unit = {
		this.updatedAt = System.currentTimeMillis
		this._blocksCount = 0
		this._hashesCount = 0
		this._emptyResponsesCount = 0
	}

	def addBlocks(v: Long): Unit = {
		this._blocksCount += v
		fixCommon(v)
	}

	def addHashes(v: Long): Unit = {
		this._hashesCount += v
		fixCommon(v)
	}

	private def fixCommon(v: Long): Unit = {
		if (v == 0) {
			this._emptyResponsesCount += 1
		}
		this.updatedAt = System.currentTimeMillis
	}

	def millisSinceLastUpdate: Long = System.currentTimeMillis - this.updatedAt

	reset()
}
