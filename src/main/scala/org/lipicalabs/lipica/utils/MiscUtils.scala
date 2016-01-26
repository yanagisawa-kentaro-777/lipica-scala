package org.lipicalabs.lipica.utils

import java.io.Closeable

import scala.annotation.tailrec

/**
 * 便利関数の置き場所。
 *
 * @since 2016/01/11
 * @author YANAGISAWA, Kentaro
 */
object MiscUtils {

	/**
	 * 渡された文字列が null もしくは空文字列である場合に真を返します。
	 *
	 * @param s 判定対象文字列。
	 * @param trim 判定前に trim するか否か。判定対象が null だったら、当然 trim しません。
	 * @return null もしくは空文字列の場合に真、そうでなければ偽。
	 */
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

	/**
	 * 渡されたリソースをクローズします。
	 * クローズ時に送出された例外は無視します。
	 *
	 * @param resource クローズ対象。
	 */
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
