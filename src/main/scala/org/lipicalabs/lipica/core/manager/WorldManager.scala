package org.lipicalabs.lipica.core.manager

import org.lipicalabs.lipica.core.base.Wallet
import org.lipicalabs.lipica.core.db.Repository
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.net.client.PeerClient
import org.lipicalabs.lipica.core.net.peer_discovery.PeerDiscovery

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 16:08
 * YANAGISAWA, Kentaro
 */
trait WorldManager {

	def activePeer: PeerClient
	def activePeer_=(v: PeerClient): Unit

	def repository: Repository

	def listener: LipicaListener

	def peerDiscovery: PeerDiscovery

	def wallet: Wallet

}
