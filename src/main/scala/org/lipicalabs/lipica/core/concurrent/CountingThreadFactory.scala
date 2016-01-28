package org.lipicalabs.lipica.core.concurrent

import java.util.concurrent.atomic.AtomicInteger

/**
 * スレッド数をカウントしながら、その数をスレッド名に反映させる ThreadFactory の実装です。
 *
 * Created by IntelliJ IDEA.
 * @author YANAGISAWA, Kentaro
 */
class CountingThreadFactory(val prefix: String) extends java.util.concurrent.ThreadFactory {
	private val count = new AtomicInteger(0)

	override def newThread(task: Runnable): Thread = {
		val threadName = "%s-%03d".format(this.prefix, this.count.getAndIncrement)
		new Thread(task, threadName)
	}
}
