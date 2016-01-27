package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

/**
 * Kademlia 的な距離モデルによるピアディスカバリーに
 * 必要なパラメータをまとめたオブジェクトです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:41
 * YANAGISAWA, Kentaro
 */
object KademliaOptions {
	val BucketSize = 16
	val Alpha = 3
	val Bins = 256
	val MaxSteps = 8

	val BucketRefreshIntervalMillis = 7200L
	val DiscoverCycleSeconds = 30L
}
