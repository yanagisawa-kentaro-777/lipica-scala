package org.lipicalabs.lipica.core.net

import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent._

import io.netty.channel.ChannelHandlerContext
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.message.{ImmutableMessages, ReasonCode, Message}
import org.lipicalabs.lipica.core.net.p2p.{DisconnectMessage, PingMessage}
import org.slf4j.LoggerFactory

/**
 * Created by IntelliJ IDEA.
 * 2015/12/05 15:25
 * YANAGISAWA, Kentaro
 */
class MessageQueue {
	import MessageQueue._

	private val messageQueue = new ConcurrentLinkedDeque[MessageRoundtrip]
	private var ctx: ChannelHandlerContext = null

	//TODO auto wiring
	private val worldManager: WorldManager = ???

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

	def disconnect(reason: ReasonCode): Unit = disconnect(new DisconnectMessage(reason))

	def disconnect(): Unit = disconnect(ImmutableMessages.DisconnectMessage)

	def receiveMessage(message: Message): Unit = {
		this.worldManager.listener.trace("[Recv: %s]".format(message))

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
		Option(messageRoundtrip).withFilter(_.retryTimes == 0).foreach {
			each => {
				val message = each.message
				this.worldManager.listener.onSendMessage(message)
				this.ctx.writeAndFlush(message)

				if (message.answerMessage.nonEmpty) {
					//送信済みとして記録する。
					messageRoundtrip.incrementRetryTimes()
					messageRoundtrip.saveTime()
				} else {
					//保持する必要がない。
					this.messageQueue.remove()
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

	private val timer = Executors.newScheduledThreadPool(4, new ThreadFactory {
		private val cnt = new AtomicInteger(0)
		override def newThread(r: Runnable) = {
			new Thread(r, "MessageQueueTimer-" + this.cnt.getAndIncrement)
		}
	})
}