package org.lipicalabs.lipica.core.net.transport.discover

import java.util.concurrent.{TimeUnit, Executors}

import org.lipicalabs.lipica.core.net.transport.discover.table.KademliaOptions
import org.lipicalabs.lipica.core.utils.CountingThreadFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:24
 * YANAGISAWA, Kentaro
 */
class DiscoveryExecutor(val nodeManager: NodeManager) {

	private val discoverer = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("discoverer"))
	private val refresher = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("refresher"))

	def discover(): Unit = {
		this.discoverer.scheduleWithFixedDelay(
			new DiscoverTask(this.nodeManager), 0, KademliaOptions.DiscoverCycleSeconds, TimeUnit.SECONDS
		)
		this.refresher.scheduleWithFixedDelay(
			new RefreshTask(this.nodeManager), 0, KademliaOptions.BucketRefreshIntervalMillis, TimeUnit.MILLISECONDS
		)
	}

}
