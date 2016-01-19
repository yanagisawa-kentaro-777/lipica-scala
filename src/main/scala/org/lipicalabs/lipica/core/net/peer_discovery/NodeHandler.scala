package org.lipicalabs.lipica.core.net.peer_discovery

import java.net.InetSocketAddress
import java.util
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong, AtomicReference, AtomicBoolean}
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import org.lipicalabs.lipica.core.net.peer_discovery.discover.DiscoveryEvent
import org.lipicalabs.lipica.core.net.peer_discovery.message.{FindNodeMessage, NeighborsMessage, PingMessage, PongMessage}
import org.lipicalabs.lipica.core.net.transport._
import org.lipicalabs.lipica.core.utils.{CountingThreadFactory, ImmutableBytes}
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/13 13:25
 * YANAGISAWA, Kentaro
 */
class NodeHandler(val node: Node, val nodeManager: NodeManager) {

	import NodeHandler._

	private val stateRef: AtomicReference[State] = new AtomicReference(State.Init)
	def state: State = this.stateRef.get

	val nodeStatistics = new NodeStatistics(this.node)

	private val waitForPongRef = new AtomicBoolean(false)
	def waitForPong: Boolean = this.waitForPongRef.get

	private val pingSentRef: AtomicLong = new AtomicLong(0L)
	private def pingSent: Long = this.pingSentRef.get

	private val pingTrialsRef: AtomicInteger = new AtomicInteger(3)
	private def pingTrials: Int = this.pingTrialsRef.get

	private val replaceCandidateRef: AtomicReference[NodeHandler] = new AtomicReference[NodeHandler](null)
	private def replaceCandidate: NodeHandler = this.replaceCandidateRef.get

	changeState(State.Discovered)


	def inetSocketAddress: InetSocketAddress = this.node.address

	private def challengeWith(aCandidate: NodeHandler): Unit = {
		this.replaceCandidateRef.set(aCandidate)
		changeState(State.EvictCandidate)
	}

	private def changeState(aNewState: State): Unit = {
		val oldState = this.state
		var newState = aNewState
		if (newState == State.Discovered) {
			sendPing()
		}
		if (newState == State.Alive) {
			this.nodeManager.table.addNode(this.node) match {
				case Some(evictCandidate) =>
					val evictHandler = this.nodeManager.getNodeHandler(evictCandidate)
					if (evictHandler.state == State.EvictCandidate) {
						//すでに決闘中なので待つ。
						aliveNodes.add(this)
					} else {
						evictHandler.challengeWith(this)
					}
				case None =>
					newState = State.Active
			}
		}
		if (newState == State.Active) {
			if (oldState == State.Alive) {
				//決闘に勝った。
				this.nodeManager.table.addNode(this.node)
			} else {
				//
			}
		}
		if (newState == State.NonActive) {
			if (oldState == State.EvictCandidate) {
				this.nodeManager.table.dropNode(this.node)
				this.replaceCandidate.changeState(State.Active)
			} else {
				//
			}
		}
		if (newState == State.EvictCandidate) {
			//生き残るためにpingを送る。
			sendPing()
		}
		this.stateRef.set(newState)
		stateChanged(oldState, newState)
	}

	private def stateChanged(oldState: State, newState: State): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> State changed %s -> %s".format(oldState, newState))
		}
		this.nodeManager.stateChanged(this, oldState, newState)
	}

	def handlePing(message: PingMessage): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Received [PING] " + this)
		}
		this.nodeStatistics.discoverInPing.add
		if (this.nodeManager.table.node != this.node) {
			sendPong(message.mdc)
		}
	}

	def handlePong(message: PongMessage): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Received [PONG] " + this)
		}
		if (this.waitForPong) {
			this.waitForPongRef.set(false)
			this.nodeStatistics.discoverInPong.add
			this.nodeStatistics.discoverMessageLatency.add(System.currentTimeMillis - this.pingSent)
			changeState(State.Alive)
		}
	}

	def handleNeighbours(message: NeighborsMessage): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Received [NEIGHBOURS] %s, Count=%,d".format(this, message.nodes.size))
		}
		this.nodeStatistics.discoverInNeighbours.add
		for (n <- message.nodes) {
			this.nodeManager.getNodeHandler(n)
		}
	}

	def handleFindNode(message: FindNodeMessage): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Received [FIND_NODE] %s".format(this))
		}
		this.nodeStatistics.discoverInFind.add
		val closestNodes = this.nodeManager.table.getClosestNodes(message.target)
		sendNeighbours(closestNodes)
	}

	def handleTimedOut(): Unit = {
		this.waitForPongRef.set(false)
		this.pingTrialsRef.decrementAndGet
		if (0 < this.pingTrials) {
			sendPing()
		} else {
			if (this.state == State.Discovered) {
				changeState(State.Dead)
			} else if (this.state == State.EvictCandidate) {
				changeState(State.NonActive)
			} else {
				//
			}
		}
	}

	def sendPing(): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Sending [PING] %s".format(this))
		}
		//自ノードのアドレス。
		val srcAddress = this.nodeManager.table.node.address
		//相手ノードのアドレス。
		val destAddress = this.node.address
		val ping = PingMessage.create(srcAddress, destAddress, this.nodeManager.key)
		this.waitForPongRef.set(true)
		this.pingSentRef.set(System.currentTimeMillis)

		sendMessage(ping)
		this.nodeStatistics.discoverOutPing.add
		pongTimer.schedule(
			new Runnable {
				override def run(): Unit = {
					if (waitForPong) {
						waitForPongRef.set(false)
						handleTimedOut()
					}
				}
			}, PingTimeout, TimeUnit.MILLISECONDS
		)
	}

	def sendPong(mdc: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Sending [PONG] %s".format(this))
		}
		val pong = PongMessage.create(mdc, this.node.address, this.nodeManager.key)
		sendMessage(pong)
		this.nodeStatistics.discoverOutPong.add
	}

	def sendNeighbours(seq: Seq[Node]): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Sending [NEIGHBOURS] %s".format(this))
		}
		val neighbours = NeighborsMessage.create(seq, this.nodeManager.key)
		sendMessage(neighbours)
		this.nodeStatistics.discoverOutNeighbours.add
	}

	def sendFindNode(target: NodeId): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<NodeHandler> Sending [FIND_NODE] %s".format(this))
		}
		val findNode = FindNodeMessage.create(target, this.nodeManager.key)
		sendMessage(findNode)
		this.nodeStatistics.discoverOutFind.add
	}

	private def sendMessage(message: TransportMessage): Unit = {
		this.nodeManager.sendOutbound(new DiscoveryEvent(message, inetSocketAddress))
	}

	override def toString: String = {
		"NodeHandler[State=%s, Address=%s, ID=%s]".format(this.state, this.node.address, this.node.id.toShortString)
	}
}

object NodeHandler {
	private val logger = LoggerFactory.getLogger("discovery")

	val aliveNodes: util.Queue[NodeHandler] = new util.ArrayDeque[NodeHandler]
	private val pongTimer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new CountingThreadFactory("pong-timer"))
	private val PingTimeout: Long = 15000L

	sealed trait State

	object State {

		case object Init extends State

		/**
		 * 新たのノードが発見されたばかりの状態。
		 * Neighboursメッセージに含まれていたか、Pingを受け取ったか。
		 * いずれにせよ、自ノードからPingを送り、その結果に応じて
		 * Alive もしくは Dead に状態を変える。
		 */
		case object Discovered extends State

		case object Dead extends State

		/**
		 * 生きている。
		 * bucketに空きがあれば、tableに登録される。
		 * 空きがなければ、既存ノードと交代するかもしれない。
		 */
		case object Alive extends State

		/**
		 * tableに登録されている。
		 */
		case object Active extends State

		/**
		 * tableに登録されているが、新たなノードと対決中。
		 * 結果によって追い出されるかも。
		 */
		case object EvictCandidate extends State

		/**
		 * 負けて追い出された。
		 */
		case object NonActive extends State
	}
}