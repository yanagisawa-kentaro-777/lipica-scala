package org.lipicalabs.lipica.core.net.server

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager
import org.lipicalabs.lipica.core.net.transport.discover.DiscoveryExecutor
import org.lipicalabs.lipica.core.utils.CountingThreadFactory
import org.slf4j.LoggerFactory

/**
 * UDPデータグラムの送受信機構です。
 * Peer Discovery における相互確認は、UDPによって行われます。
 * 自ノード全体で１個のインスタンスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/26 12:36
 * YANAGISAWA, Kentaro
 */
class UDPListener {

	import UDPListener._

	private val address: String = SystemProperties.CONFIG.bindAddress
	private val port: Int = SystemProperties.CONFIG.bindPort

	private val executor = Executors.newSingleThreadExecutor(new CountingThreadFactory("udp-starter"))

	def start(): Boolean = {
		if (SystemProperties.CONFIG.peerDiscoveryEnabled) {
			val task = new Runnable {
				override def run(): Unit = {
					try {
						UDPListener.this.bind()
					} catch {
						case e: Exception =>
							logger.warn("<UDPListener> Exception (%s) caught in binding [%s]:%d".format(e.getClass.getSimpleName, address, port), e)
					}
				}
			}
			this.executor.execute(task)
			//TODO bind完了をFutureで待てるようにする。
			true
		} else {
			logger.info("<UDPListener> Peer discovery is not enabled. Not binding.")
			false
		}
	}

	private def bind(): Unit = {
		val group = new NioEventLoopGroup(1, new CountingThreadFactory("udp-listener"))
		val nodeManager = WorldManager.instance.nodeManager
		val discoverExecutor = new DiscoveryExecutor(nodeManager)
		val startedRef: AtomicBoolean = new AtomicBoolean(false)

		try {
			while (true) {
				val b = new Bootstrap
				b.group(group).channel(classOf[NioDatagramChannel]).handler(new ChannelInitializer[NioDatagramChannel] {
					override def initChannel(ch: NioDatagramChannel): Unit = {
						ch.pipeline.addLast(new PacketDecoder)
						val messageHandler = new MessageHandler(ch, nodeManager)
						nodeManager.setMessageSender(messageHandler)
						ch.pipeline.addLast(messageHandler)
					}
				})
				val channel = b.bind(this.address, this.port).sync().channel()
				logger.info("<UDPListener> Bound on address [%s]:%d".format(this.address, this.port))

				if (startedRef.compareAndSet(false, true)) {
					logger.info("<UDPListener> Starting discovery tasks. [%s]:%d".format(this.address, this.port))
					discoverExecutor.discover()
				}

				//このチャネルが切断されるまで待つ。
				channel.closeFuture().sync()
				logger.info("<UDPListener> Stopped binding on [%s]:%d".format(this.address, this.port))
				//切断されたとしたら、例外の処理を誤ってスタックフレームを遡行したことによるものであろうから、
				//しばらく待って再度 bind する。
				Thread.sleep(5000L)
			}
		} catch {
			case any: Throwable =>
				//ここに来るのは想定外である。
				logger.warn("<UDPListener> Exception caught: %s".format(any.getClass.getSimpleName), any)
		} finally {
			group.shutdownGracefully()
		}
	}

}

object UDPListener {
	private val logger = LoggerFactory.getLogger("net")
}