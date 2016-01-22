package org.lipicalabs.lipica

import java.net.InetSocketAddress
import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicBoolean

import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.rest.RestApiServer
import org.slf4j.LoggerFactory

/**
 * プロセスを起動するエントリーポイントです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/26 10:06
 * YANAGISAWA, Kentaro
 */
object EntryPoint {
	private val logger = LoggerFactory.getLogger("general")

	private val isShutdownRef = new AtomicBoolean(false)
	def isShutdown: Boolean = this.isShutdownRef.get

	def main(args: Array[String]): Unit = {
		startup(args)
	}

	def startup(args: Array[String]): Unit = {
		//設定オブジェクトを生成する。
		val configFilePath = args(0).trim
		NodeProperties.loadFromFile(Paths.get(configFilePath))

		//ノード本体を起動する。
		Lipica.startup()

		//REST APIサーバーを起動する。
		val config = NodeProperties.CONFIG
		if (config.restApiEnabled) {
			val webBindAddress = new InetSocketAddress(config.restApiBindAddress, config.restApiBindPort)
			RestApiServer.startup(webBindAddress)
		}

		//ShutdownHookを登録する。
		Runtime.getRuntime.addShutdownHook(new Thread() {
			override def run(): Unit = shutdown()
		})
	}


	def shutdown(): Unit = {
		this.synchronized {
			if (this.isShutdown) {
				return
			}
			logger.info("<EntryPoint> SHUTTING DOWN.")
			//ノード本体を停止させる。
			Lipica.shutdown()
			//REST APIサーバーを停止させる。
			RestApiServer.shutdown()
			this.isShutdownRef.set(true)
			logger.info("<EntryPoint> SHUTDOWN COMPLETE.")
		}
	}

}
