package org.lipicalabs.lipica.core.net

import org.lipicalabs.lipica.core.net.message.Message

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 15:21
 * YANAGISAWA, Kentaro
 */
class MessageRoundtrip(val message: Message) {

	private var _lastTimestamp = 0L
	def lastTimestamp: Long = this._lastTimestamp
	def saveTime(): Unit = {
		this._lastTimestamp = System.currentTimeMillis()
	}
	def hasToRetry: Boolean = {
		20000L < (System.currentTimeMillis() - this._lastTimestamp)
	}

	private var _retryTimes = 0L
	def retryTimes: Long = this._retryTimes
	def incrementRetryTimes(): Unit = {
		this._retryTimes += 1
	}

	private var _answered = false
	def isAnswered = this._answered
	def answer(): Unit = this._answered = true

	saveTime()
}
