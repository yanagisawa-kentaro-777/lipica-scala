package org.lipicalabs.lipica.core.net.transport.discover

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioDatagramChannel
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.manager.WorldManager

/**
 * Created by IntelliJ IDEA.
 * 2015/12/26 12:36
 * YANAGISAWA, Kentaro
 */
class UDPListener {

	private val address: String = SystemProperties.CONFIG.bindAddress
	private val port: Int = SystemProperties.CONFIG.bindPort

	def init(): Unit = {
		if (SystemProperties.CONFIG.peerDiscoveryEnabled) {
			new Thread("UDPListener") {
				override def run(): Unit = {
					try {
						UDPListener.this.start()
					} catch {
						case e: Exception => e.printStackTrace() //TODO
					}
				}
			}.start()
		}
	}

	def start(): Unit = {
		val group = new NioEventLoopGroup(1)
		try {
			val nodeManager = WorldManager.instance.nodeManager
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
			val discoverExecutor = new DiscoveryExecutor(nodeManager)
			discoverExecutor.discover()

			channel.closeFuture().sync()
			Thread.sleep(5000L)
		} catch {
			case e: Exception => e.printStackTrace()//TODO
		} finally {
			group.shutdownGracefully()
		}
	}

}
