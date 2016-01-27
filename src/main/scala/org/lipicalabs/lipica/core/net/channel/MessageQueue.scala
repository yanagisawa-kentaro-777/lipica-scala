package org.lipicalabs.lipica.core.net.channel

import java.util.concurrent._

import io.netty.channel.ChannelHandlerContext
import org.lipicalabs.lipica.core.concurrent.ExecutorPool
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.core.net.message.Message
import org.lipicalabs.lipica.core.net.p2p.{ReasonCode, P2PMessageFactory, DisconnectMessage, PingMessage}
import org.slf4j.LoggerFactory

/**
 * １個のピアとの間のメッセージ送受信キューを表すクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/05 15:25
 * YANAGISAWA, Kentaro
 */
class MessageQueue {
	import MessageQueue._

	private val messageQueue = new ConcurrentLinkedDeque[MessageRoundtrip]
	private var ctx: ChannelHandlerContext = null

	private def componentsMotherboard: ComponentsMotherboard = ComponentsMotherboard.instance

	private var hasPing = false

	private var timerTask: ScheduledFuture[_] = null

	def activate(ctx: ChannelHandlerContext): Unit = {
		this.ctx = ctx
		this.timerTask = timer.scheduleAtFixedRate(new Runnable {
			override def run() = nudgeQueue()
		}, 10, 10, TimeUnit.MILLISECONDS)
	}

	def sendMessage(message: Message): Unit = {
		message match {
			case p: PingMessage =>
				if (this.hasPing) {
					return
				} else {
					this.hasPing = true
				}
			case _ => ()
		}
		this.messageQueue.add(new MessageRoundtrip(message))
	}

	def disconnect(message: DisconnectMessage): Unit = {
		this.ctx.writeAndFlush(message)
		this.ctx.close()
	}

	def disconnect(reason: ReasonCode): Unit = disconnect(DisconnectMessage(reason))

	def disconnect(): Unit = disconnect(P2PMessageFactory.DisconnectMessage)

	def receiveMessage(message: Message): Unit = {
		this.componentsMotherboard.listener.trace("[Recv: %s]".format(message))

		if (!messageQueue.isEmpty) {
			val messageRoundTrip = this.messageQueue.peek()
			val waitingMessage = messageRoundTrip.message

			if (waitingMessage.isInstanceOf[PingMessage]) {
				this.hasPing = false
			}
			waitingMessage.answerMessage match {
				case Some(c) =>
					if (message.getClass == c) {
						messageRoundTrip.answer()
						logger.trace("<MessageQueue> Message round trip covered: %s".format(c))
					}
				case _ => ()
			}
		}
	}

	private def removeAnsweredMessage(messageRoundtrip: MessageRoundtrip): Unit = {
		if (Option(messageRoundtrip).exists(_.isAnswered)) {
			this.messageQueue.remove()
		}
	}

	/**
	 * メッセージ処理を１つ進める。
	 */
	private def nudgeQueue(): Unit = {
		//返信待ちの記録があったら、もういらないので削除する。
		removeAnsweredMessage(this.messageQueue.peek())
		//送信すべきメッセージを送信する。
		sendToWire(this.messageQueue.peek())
	}

	private def sendToWire(messageRoundtrip: MessageRoundtrip): Unit = {
		Option(messageRoundtrip).foreach {
			each => {
				if (each.retryTimes == 0) {
					val message = each.message
					this.componentsMotherboard.listener.onSendMessage(message)
					if (logger.isDebugEnabled) {
						logger.debug("<MessageQueue> Sending %s to %s".format(message.command.getClass.getSimpleName, this.ctx.channel.remoteAddress))
					}
					//送信する。
					this.ctx.writeAndFlush(message)

					if (message.answerMessage.nonEmpty) {
						//送信済みとして記録する。
						messageRoundtrip.incrementRetryTimes()
						messageRoundtrip.saveTime()
					} else {
						//返信不要のメッセージだから、これ以上保持する必要がない。
						this.messageQueue.remove()
					}
				} else {
					//TODO retry をどうするか。
					each.incrementRetryTimes()
					if (500 <= each.retryTimes) {
						//タイムアウト扱い。
						each.answer()
					}
				}
			}
		}
	}

	def close(): Unit = {
		Option(this.timerTask).foreach(_.cancel(false))
	}

}

object MessageQueue {
	private val logger = LoggerFactory.getLogger("net")

	private val timer = ExecutorPool.instance.messageQueueProcessor

}

class MessageRoundtrip(val message: Message) {

	private var _lastTimestamp = 0L
	def lastTimestamp: Long = this._lastTimestamp
	def saveTime(): Unit = {
		this._lastTimestamp = System.currentTimeMillis()
	}
	def hasToRetry: Boolean = {
		20000L < (System.currentTimeMillis() - this._lastTimestamp)
	}

	private var _retryTimes = 0L
	def retryTimes: Long = this._retryTimes
	def incrementRetryTimes(): Unit = {
		this._retryTimes += 1
	}

	private var _answered = false
	def isAnswered = this._answered
	def answer(): Unit = this._answered = true

	saveTime()
}