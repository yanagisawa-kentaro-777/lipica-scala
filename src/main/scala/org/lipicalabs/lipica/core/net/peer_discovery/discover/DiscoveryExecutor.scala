package org.lipicalabs.lipica.core.net.peer_discovery.discover

import java.util.concurrent.TimeUnit

import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.net.peer_discovery.NodeManager
import org.lipicalabs.lipica.core.net.peer_discovery.discover.table.KademliaOptions

/**
 * ノードディスカバリのための処理を定期的に実行するクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/20 15:24
 * YANAGISAWA, Kentaro
 */
class DiscoveryExecutor(val nodeManager: NodeManager) {

	private val discoverer = ExecutorPool.instance.discoverer
	private val refresher = ExecutorPool.instance.refresher

	def discover(): Unit = {
		//自ノードの近傍のノードの情報を収集するタスク。
		this.discoverer.scheduleWithFixedDelay(
			new DiscoverTask(this.nodeManager), 0, KademliaOptions.DiscoverCycleSeconds, TimeUnit.SECONDS
		)
		//ランダムな地帯のノードの情報を収集するタスク。
		this.refresher.scheduleWithFixedDelay(
			new RefreshTask(this.nodeManager), 0, KademliaOptions.BucketRefreshIntervalMillis, TimeUnit.MILLISECONDS
		)
	}

}
