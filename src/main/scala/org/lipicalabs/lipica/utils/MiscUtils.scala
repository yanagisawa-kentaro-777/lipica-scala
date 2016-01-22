package org.lipicalabs.lipica.utils

import java.io.Closeable

import scala.annotation.tailrec

/**
 *
 * @since 2016/01/11
 * @author YANAGISAWA, Kentaro
 */
object MiscUtils {

	def isNullOrEmpty(s: String, trim: Boolean): Boolean = {
		if (s eq null) {
			return true
		}
		if (trim) {
			s.trim.isEmpty
		} else {
			s.isEmpty
		}
	}

	def closeIfNotNull(resource: Closeable): Unit = {
		if (resource eq null) {
			return
		}
		try {
			resource.close()
		} catch {
			case any: Throwable => ()
		}
	}

	/**
	 * 現在動作しているすべてのスレッドの数を返します。
	 *
	 * 返される結果は概要に過ぎず、何らかの時点での厳密なスナップショットではありません。
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
			//これが最上位である。
			group
		} else {
			//さらに上をたどる。
			getRootThreadGroup(parent)
		}
	}

}
