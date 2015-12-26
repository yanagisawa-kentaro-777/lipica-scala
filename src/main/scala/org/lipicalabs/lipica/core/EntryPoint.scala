package org.lipicalabs.lipica.core

import java.nio.file.Paths

import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.facade.Lipica

/**
 * Created by IntelliJ IDEA.
 * 2015/12/26 10:06
 * YANAGISAWA, Kentaro
 */
object EntryPoint {

	def main(args: Array[String]): Unit = {
		val configFilePath = args(0).trim
		SystemProperties.loadFromFile(Paths.get(configFilePath))

		val lipica = Lipica.create
		//lipica.getBlockLoader.loadBlocks()
		lipica.startPeerDiscovery()
	}

}
