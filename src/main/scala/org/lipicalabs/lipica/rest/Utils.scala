package org.lipicalabs.lipica.rest

import scala.annotation.tailrec

/**
 * Created by IntelliJ IDEA.
 * 2016/01/06 10:56
 * YANAGISAWA, Kentaro
 */
object Utils {

	/**
	 * 現在動作しているすべてのスレッドの数を返します。
	 */
	def numberOfThreads: Int = {
		val rootGroup = getRootThreadGroup(Thread.currentThread.getThreadGroup)
		rootGroup.activeCount
	}

	/**
	 * 現在動作しているすべてのスレッドを配列に格納して返します。
	 *
	 * 返される結果は概要に過ぎず、何らかの時点での厳密なスナップショットではありません。
	 */
	def allThreads: Array[Thread] = {
		val rootGroup = getRootThreadGroup(Thread.currentThread.getThreadGroup)
		val result = new Array[Thread](rootGroup.activeCount)
		rootGroup.enumerate(result)
		result
	}

	/**
	 * 渡されたスレッドグループを起点として、
	 * root に当たる最上位のスレッドグループを探索し、結果を返します。
	 */
	@tailrec
	private def getRootThreadGroup(group: ThreadGroup): ThreadGroup = {
		val parent = group.getParent
		if (parent eq null) {
			group
		} else {
			getRootThreadGroup(parent)
		}
	}

}
