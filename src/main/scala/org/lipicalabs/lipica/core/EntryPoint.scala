package org.lipicalabs.lipica.core

import org.lipicalabs.lipica.core.facade.Lipica

/**
 * Created by IntelliJ IDEA.
 * 2015/12/26 10:06
 * YANAGISAWA, Kentaro
 */
object EntryPoint {

	def main(args: Array[String]): Unit = {
		val lipica = Lipica.create
		//lipica.getBlockLoader.loadBlocks()
		lipica.startPeerDiscovery()
	}

}
