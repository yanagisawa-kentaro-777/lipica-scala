package org.lipicalabs.lipica.core.manager

import org.lipicalabs.lipica.core.base._
import org.lipicalabs.lipica.core.db.{BlockStore, RepositoryImpl, Repository}
import org.lipicalabs.lipica.core.listener.LipicaListener
import org.lipicalabs.lipica.core.net.client.PeerClient
import org.lipicalabs.lipica.core.net.lpc.message.StatusMessage
import org.lipicalabs.lipica.core.net.lpc.sync.SyncManager
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.HelloMessage
import org.lipicalabs.lipica.core.net.peer_discovery.PeerDiscovery
import org.lipicalabs.lipica.core.net.server.ChannelManager
import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.net.transport.discover.NodeManager

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 16:08
 * YANAGISAWA, Kentaro
 */
trait WorldManager {

	def listener: LipicaListener

	def blockchain: Blockchain

	def repository: Repository

	def wallet: Wallet

	def activePeer: PeerClient

	def activePeer_=(v: PeerClient): Unit

	def peerDiscovery: PeerDiscovery

	def blockStore: BlockStore

	def channelManager: ChannelManager

	def adminInfo: AdminInfo

	def nodeManager: NodeManager

	def syncManager: SyncManager


	def init(): Unit//TODO post construct

	def addListener(listener: LipicaListener): Unit

	def startPeerDiscovery(): Unit

	def stopPeerDiscovery(): Unit

	def loadBlockchain(): Unit

	def close(): Unit
}
