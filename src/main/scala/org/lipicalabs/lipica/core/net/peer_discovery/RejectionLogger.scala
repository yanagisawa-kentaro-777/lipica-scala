package org.lipicalabs.lipica.core.net.peer_discovery

import java.util.concurrent.{ThreadPoolExecutor, RejectedExecutionHandler}

import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/14 20:40
 * YANAGISAWA, Kentaro
 */
class RejectionLogger extends RejectedExecutionHandler {
	import RejectionLogger._

	override def rejectedExecution(r: Runnable, executor: ThreadPoolExecutor): Unit = {
		logger.warn("%s is rejected.".format(r))
	}

}

object RejectionLogger {
	private val logger = LoggerFactory.getLogger("wire")
}