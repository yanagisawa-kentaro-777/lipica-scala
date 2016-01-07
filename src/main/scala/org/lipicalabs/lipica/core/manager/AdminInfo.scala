package org.lipicalabs.lipica.core.manager

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by IntelliJ IDEA.
 * 2015/11/27 10:43
 * YANAGISAWA, Kentaro
 */
class AdminInfo {

	val startupTimeStamp = System.currentTimeMillis

	private val consensusRef = new AtomicBoolean(true)

//	private val blockExecTimes = new util.LinkedList[Long]


	def lostConsensus(): Unit = this.consensusRef.set(false)

	def isConsensusKept: Boolean = this.consensusRef.get

//	def addBlockExecNanos(nanos: Long): Unit = {
//		this.blockExecTimes.add(nanos)
//	}
//
//	def getExecAverage: Long = {
//		if (this.blockExecTimes.isEmpty) {
//			return 0L
//		}
//		import scala.collection.JavaConversions._
//		this.blockExecTimes.sum / this.blockExecTimes.size
//	}

}
