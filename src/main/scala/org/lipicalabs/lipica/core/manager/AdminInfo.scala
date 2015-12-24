package org.lipicalabs.lipica.core.manager

import java.util


/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:43
 * YANAGISAWA, Kentaro
 */
class AdminInfo {

	val startupTimeStamp = System.currentTimeMillis

	private var _consensus = true

	private val blockExecTimes = new util.LinkedList[Long]


	def lostConsensus(): Unit = {
		this._consensus = false
	}

	def isConsensusKept: Boolean = this._consensus

	def addBlockExecNanos(nanos: Long): Unit = {
		this.blockExecTimes.add(nanos)
	}

	def getExecAverage: Long = {
		if (this.blockExecTimes.isEmpty) {
			return 0L
		}
		import scala.collection.JavaConversions._
		this.blockExecTimes.sum / this.blockExecTimes.size
	}

}
