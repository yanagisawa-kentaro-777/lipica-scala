package org.lipicalabs.lipica.core

import java.nio.file.Paths

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.facade.Lipica
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/26 10:06
 * YANAGISAWA, Kentaro
 */
object EntryPoint {
	private val logger = LoggerFactory.getLogger("general")

	def main(args: Array[String]): Unit = {
		val configFilePath = args(0).trim
		SystemProperties.loadFromFile(Paths.get(configFilePath))

		val lipica = Lipica.create

//		Runtime.getRuntime.addShutdownHook(new Thread() {
//			override def run(): Unit = {
//				logger.info("<EntryPoint> SHUTTING DOWN: Closing.")
//				lipica.close()
//				logger.info("<EntryPoint> Closed.")
//			}
//		})

		lipica.startPeerDiscovery()
	}

}
