package org.lipicalabs.lipica.core.net.lpc.handler

import io.netty.channel.{ChannelHandlerContext, SimpleChannelInboundHandler}
import org.lipicalabs.lipica.core.base.{Block, BlockChain, TransactionLike}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.MessageQueue
import org.lipicalabs.lipica.core.net.lpc.{LpcMessageCode, LpcVersion}
import org.lipicalabs.lipica.core.net.lpc.message._
import org.lipicalabs.lipica.core.net.lpc.sync._
import org.lipicalabs.lipica.core.net.message.ReasonCode
import org.lipicalabs.lipica.core.net.server.Channel
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.slf4j.LoggerFactory

import scala.collection.{JavaConversions, mutable}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/10 20:05
 * YANAGISAWA, Kentaro
 */
abstract class LpcHandler(override val version: LpcVersion) extends SimpleChannelInboundHandler[LpcMessage] with Lpc {

	import LpcHandler._
	//TODO auto wiring
	protected val blockchain: BlockChain = ???
	protected val syncQueue: SyncQueue = ???
	protected val worldManager: WorldManager = ???

	protected var _channel: Channel = null
	def channel: Channel = this._channel
	def channel_=(v: Channel): Unit = this._channel = v

	private var _messageQueue: MessageQueue = null
	def messageQueue: MessageQueue = this._messageQueue
	def messageQueue_=(v: MessageQueue): Unit = this._messageQueue = v

	protected var lpcState: LpcState = LpcState.Init

	protected var _peerDiscoveryMode: Boolean = false
	def peerDiscoveryMode: Boolean = this._peerDiscoveryMode
	def peerDiscoveryMode_=(v: Boolean): Unit = this._peerDiscoveryMode = v

	private var blocksLackHits: Int = 0

	protected var syncState: SyncStateName = Idle
	protected var syncDone: Boolean = false


	protected var _processTransactions: Boolean = true
	override def enableTransactions() = this._processTransactions = true
	override def disableTransactions() = this._processTransactions = false

	protected var bestHash: ImmutableBytes = null
	override def bestKnownHash = this.bestHash

	protected var _lastHashToAsk: ImmutableBytes = null
	override def lastHashToAsk = this._lastHashToAsk
	override def lastHashToAsk_=(v: ImmutableBytes) = this._lastHashToAsk = v

	protected var _maxHashesAsk = SystemProperties.CONFIG.maxHashesAsk
	override def maxHashesAsk = this._maxHashesAsk
	override def maxHashesAsk_=(v: Int) = this._maxHashesAsk = v

	protected val sentHashes: mutable.Buffer[ImmutableBytes] = JavaConversions.asScalaBuffer(java.util.Collections.synchronizedList(new java.util.ArrayList[ImmutableBytes]))
	protected val syncStats: SyncStatistics = new SyncStatistics


	override def channelRead0(ctx: ChannelHandlerContext, message: LpcMessage) = {
		if (LpcMessageCode.inRange(message.command.asByte)) {
			loggerNet.trace("<LpcHandler> Invoke: %s".format(message.command))
		}
		this.worldManager.listener.trace("<LpcHandler> Invoke: %s".format(message.command))
		channel.nodeStatistics.lpcInbound.add

		message.command match {
			case LpcMessageCode.Status =>
				this.messageQueue.receiveMessage(message)
				onStatusReceived(message.asInstanceOf[StatusMessage], ctx)
			case LpcMessageCode.NewBlockHashes =>
				this.messageQueue.receiveMessage(message)
				processNewBlockHashes(message.asInstanceOf[NewBlockHashesMessage])
			case LpcMessageCode.Transactions =>
				this.messageQueue.receiveMessage(message)
				processTransactions(message.asInstanceOf[TransactionsMessage])
			case LpcMessageCode.GetBlockHashes =>
				this.messageQueue.receiveMessage(message)
				processGetBlockHashes(message.asInstanceOf[GetBlockHashesMessage])
			case LpcMessageCode.BlockHashes =>
				this.messageQueue.receiveMessage(message)
				onBlockHashes(message.asInstanceOf[BlockHashesMessage])
			case LpcMessageCode.GetBlocks =>
				this.messageQueue.receiveMessage(message)
				processGetBlocks(message.asInstanceOf[GetBlocksMessage])
			case LpcMessageCode.Blocks =>
				this.messageQueue.receiveMessage(message)
				processBlocks(message.asInstanceOf[BlocksMessage])
			case LpcMessageCode.NewBlock =>
				this.messageQueue.receiveMessage(message)
				processNewBlock(message.asInstanceOf[NewBlockMessage])
			case LpcMessageCode.GetBlockHashesByNumber =>
				this.messageQueue.receiveMessage(message)
				processGetBlockHashesByNumber(message.asInstanceOf[GetBlockHashesByNumberMessage])
			case _ =>
				()
		}
	}

	override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
		loggerNet.warn("<LpcHandler> Exception caught.", cause)
		onShutdown()
		ctx.close()
	}

	override def handlerRemoved(ctx: ChannelHandlerContext): Unit = {
		loggerNet.debug("<LpcHandler> Handler removed.")
		onShutdown()
	}

	def activate(): Unit = {
		loggerNet.info("<LpcHandler> LPC protocol activated.")
		this.worldManager.listener.trace("LPC protocol activated.")
		sendStatus()
	}

	protected def disconnect(reason: ReasonCode): Unit = {
		this.messageQueue.disconnect(reason)
		this.channel.nodeStatistics.nodeDisconnectedLocal(reason)
	}

	private def onStatusReceived(message: StatusMessage, ctx: ChannelHandlerContext): Unit = {
		this.channel.nodeStatistics.lpcHandshake(message)
		this.worldManager.listener.onLpcStatusUpdated(this.channel.node, message)

		if ((message.genesisHash != BlockChain.GenesisHash) || (message.protocolVersion != this.version.code)) {
			loggerNet.info("<LpcHandler> Removing handler for %s due to protocol incompatibility.".format(ctx.channel().remoteAddress()))
			this.lpcState = LpcState.Failed
			disconnect(ReasonCode.IncompatibleProtocol)
			ctx.pipeline().remove(this)
			return
		}
		if (message.networkId != SystemProperties.CONFIG.networkId) {
			this.lpcState = LpcState.Failed
			disconnect(ReasonCode.NullIdentity)
			return
		}
		if (this.peerDiscoveryMode) {
			loggerNet.debug("<LpcHandler> Peer discovery mode: Status received. disconnecting...")
			disconnect(ReasonCode.Requested)
			ctx.close().sync()
			ctx.disconnect().sync()
			return
		}
		this.lpcState = LpcState.Succeeded
		processStatus(message)
	}

	protected def processStatus(message: StatusMessage): Unit = {
		this.bestHash = message.bestHash
	}

	protected def sendStatus(): Unit = {
		val protocolVersion = this.version.code.toByte
		val newtworkdId = SystemProperties.CONFIG.networkId
		val totalDifficulty = this.blockchain.totalDifficulty
		val bestHash = this.blockchain.bestBlockHash
		val message = StatusMessage(protocolVersion, newtworkdId, ImmutableBytes.asUnsignedByteArray(totalDifficulty), bestHash, BlockChain.GenesisHash)
		sendMessage(message)
	}

	protected def processNewBlockHashes(message: NewBlockHashesMessage): Unit = {
		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: processing NEW block hashes: %,d".format(this.channel.peerIdShort, message.blockHashes.size))
		}
		message.blockHashes.lastOption.foreach {
			last => {
				this.bestHash = last
				this.syncQueue.addNewBlockHashes(message.blockHashes)
			}
		}
	}

	override def sendTransaction(tx: TransactionLike) = {
		val message = TransactionsMessage(Seq(tx))
		sendMessage(message)
	}

	protected def processTransactions(message: TransactionsMessage): Unit = {
		if (!this._processTransactions) {
			return
		}
		this.blockchain.addPendingTransactions(message.transactions.toSet)
		for (tx <- message.transactions) {
			this.worldManager.wallet.addTransaction(tx)
		}
	}

	protected def sendGetBlockHashes(): Unit = {
		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: send GetBlockHashes. Hash=%s, MaxAsk=%,d".format(this.channel.peerIdShort, this.lastHashToAsk, this.maxHashesAsk))
		}
		sendMessage(new GetBlockHashesMessage(this.lastHashToAsk, this.maxHashesAsk))
	}

	protected def processGetBlockHashes(message: GetBlockHashesMessage): Unit = {
		val hashes = this.blockchain.getSeqOfHashesStartingFrom(message.bestHash, message.maxBlocks max SystemProperties.CONFIG.maxHashesAsk)
		sendMessage(BlockHashesMessage(hashes))
	}

	protected def processBlockHashes(hashes: Seq[ImmutableBytes]): Unit

	protected def onBlockHashes(message: BlockHashesMessage): Unit = {
		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: processing block hashes. Size=%,d".format(this.channel.peerIdShort, message.blockHashes.size))
		}
		if (this.syncState != HashRetrieving) {
			return
		}
		val receivedHashes = message.blockHashes
		if (receivedHashes.isEmpty && this.syncDone) {
			changeState(DoneHashRetrieving)
		} else {
			this.syncStats.addHashes(receivedHashes.size)
			processBlockHashes(receivedHashes)
		}
		if (loggerSync.isInfoEnabled)
			if (this.syncState == DoneHashRetrieving) {
				loggerSync.info("<LpcHandler> Peer %s: hashes sync completed %,d hashes in queue.".format(this.channel.peerIdShort, this.syncQueue.hashStoreSize))
			} else {
				this.syncQueue.logHashQueueSize()
			}
	}

	protected def sendGetBlocks: Boolean = {
		val hashes = this.syncQueue.pollHashes
		if (hashes.isEmpty) {
			if (loggerSync.isInfoEnabled) {
				loggerSync.info("<LpcHandler> Peer %s: no more hashes in the queue. Idle.".format(this.channel.peerIdShort))
			}
			changeState(Idle)
			return false
		}
		this.sentHashes.clear()
		hashes.foreach(each => this.sentHashes.append(each))

		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: sending GetBlocks count=%,d".format(this.channel.peerIdShort, this.sentHashes.size))
		}

		val shuffled = scala.util.Random.shuffle(hashes.toSeq)
		sendMessage(GetBlocksMessage(shuffled))
		true
	}

	protected def processGetBlocks(message: GetBlocksMessage): Unit = {
		val blocks = message.blockHashes.flatMap(each => this.blockchain.getBlockByHash(each))
		sendMessage(BlocksMessage(blocks))
	}


	protected def processBlocks(message: BlocksMessage): Unit = {
		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: blocks. Size=%,d".format(this.channel.peerIdShort, message.blocks.size))
		}
		this.syncStats.addBlocks(message.blocks.size)

		removeFromSentHashes(BlockChain.GenesisHash)
		message.blocks.foreach(each => removeFromSentHashes(each.hash))
		returnHashes()

		if (message.blocks.nonEmpty) {
			for (block <- message.blocks) {
				if (this.channel.totalDifficulty < block.difficultyAsBigInt) {
					this.bestHash = block.hash
					this.channel.nodeStatistics.lpcTotalDifficulty = block.difficultyAsBigInt
				}
			}
			this.syncQueue.addBlocks(message.blocks, this.channel.nodeId)
			this.syncQueue.logHashQueueSize()
		} else {
			changeState(BlocksLack)
		}
		if (this.syncState == BlockRetrieving) {
			sendGetBlocks
		}
	}

	private def removeFromSentHashes(hash: ImmutableBytes): Unit = {
		this.sentHashes.indices.find(i => this.sentHashes(i) == hash).foreach(idx => this.sentHashes.remove(idx))
	}

	def sendNewBlock(block: Block): Unit = {
		sendMessage(NewBlockMessage(block, block.difficulty))
	}

	protected def processNewBlock(message: NewBlockMessage): Unit = {
		val newBlock = message.block
		loggerSync.info("<LpcHandler> New block received: index=%,d".format(newBlock.blockNumber))

		this.channel.nodeStatistics.lpcTotalDifficulty = message.difficulty.toPositiveBigInt
		this.bestHash = newBlock.hash

		this.syncQueue.addNewBlock(newBlock, channel.nodeId)
		this.syncQueue.logHashQueueSize()
	}

	protected def processGetBlockHashesByNumber(message: GetBlockHashesByNumberMessage): Unit

	protected def sendMessage(message: LpcMessage): Unit = {
		this.messageQueue.sendMessage(message)
		this.channel.nodeStatistics.lpcOutbound.add
	}

	def startHashRetrieving(): Unit

	override def changeState(aNewState: SyncStateName): Unit = {
		if (this.syncState == aNewState) {
			return
		}
		if (loggerSync.isTraceEnabled) {
			loggerSync.trace("<LpcHandler> Peer %s: changing state from %s to %s".format(this.channel.peerIdShort, this.syncState, aNewState))
		}
		var newState = aNewState
		if (newState == HashRetrieving) {
			this.syncStats.reset()
			startHashRetrieving()
		}
		if (newState == BlockRetrieving) {
			this.syncStats.reset()
			val sent = sendGetBlocks
			if (!sent) {
				newState = Idle
			}
		}
		if (newState == BlocksLack) {
			if (this.syncDone) {
				return
			}
			this.blocksLackHits += 1
			if (BlocksLackMaxHits < this.blocksLackHits) {
				return
			}
		}
		this.syncState = newState
	}

	override def isHashRetrievingDone = this.syncState == DoneHashRetrieving

	override def isHashRetrieving = this.syncState == HashRetrieving

	override def hasBlocksLack = this.syncState == BlocksLack

	override def hasStatusPassed = this.lpcState != LpcState.Init

	override def hasStatusSucceeded = this.lpcState == LpcState.Succeeded

	override def onShutdown() = {
		changeState(Idle)
		returnHashes()
	}

	override def isIdle = this.syncState == Idle

	override def onSyncDone() = {
		this.syncDone = true
	}


	override def logSycStats() = {
		//TODO 未実装。
	}

	override def getSyncStats = this.syncStats

	protected def returnHashes(): Unit = {
		this.sentHashes.synchronized {
			this.syncQueue.returnHashes(this.sentHashes.toIterable)
		}
		this.sentHashes.clear()
	}

}

object LpcHandler {
	private val loggerNet = LoggerFactory.getLogger("net")
	private val loggerSync = LoggerFactory.getLogger("sync")

	private val BlocksLackMaxHits = 5

}

sealed trait LpcState

object LpcState {
	case object Init extends LpcState
	case object Succeeded extends LpcState
	case object Failed extends LpcState
}