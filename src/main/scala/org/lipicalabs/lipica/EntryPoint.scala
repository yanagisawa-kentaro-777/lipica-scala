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

	/**
	 * エントリーポイント。
	 * @param args １個目の引数に、設定ファイルのパスを表す文字列が必要。
	 */
	def main(args: Array[String]): Unit = {
		startup(args)
	}

	private val isStartedRef = new AtomicBoolean(false)
	private def isStarted: Boolean = this.isStartedRef.get

	/**
	 * ノードおよびAPIサーバーを起動します。
	 */
	def startup(args: Array[String]): Unit = {
		if (args.isEmpty) {
			throw new IllegalArgumentException("You need at least one argument: Config file path.")
		}

		this.synchronized {
			if (this.isStarted) {
				return
			}
			logger.info("<EntryPoint> STARTING UP.")

			//設定オブジェクトを生成する。
			val configFilePath = args(0).trim
			NodeProperties.loadFromFile(Paths.get(configFilePath))

			//ノード本体を起動する。
			Lipica.startup()

			//REST APIサーバーを起動する。
			val config = NodeProperties.instance
			if (config.restApiEnabled) {
				val webBindAddress = new InetSocketAddress(config.restApiBindAddress, config.restApiBindPort)
				RestApiServer.startup(webBindAddress)
			}

			//ShutdownHookを登録する。
			Runtime.getRuntime.addShutdownHook(new Thread() {
				override def run(): Unit = EntryPoint.shutdown()
			})

			this.isStartedRef.set(true)
			logger.info("<EntryPoint> STARTUP COMPLETE.")

			//sample.
			val lipica = Lipica.instance
			lipica.addListener(new SampleListener(lipica))
		}
	}

	private val isShutdownRef = new AtomicBoolean(false)
	def isShutdown: Boolean = this.isShutdownRef.get

	/**
	 * ノードおよびAPIサーバーの動作を停止させます。
	 */
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
