package org.lipicalabs.lipica.rest

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}

/**
 * API用のWebサーバーのインスタンス保持と、
 * ライフサイクル管理を行うためのオブジェクトです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/28 13:46
 * YANAGISAWA, Kentaro
 */
object RestApiServer {

	private val serverRef = new AtomicReference[Server](null)

	/**
	 * 渡されたアドレスを bind するWebサーバーを起動し、
	 * そのインスタンスを返します。
	 */
	def startup(bindAddress: InetSocketAddress): Option[Server] = {
		this.synchronized {
			if (Option(this.serverRef.get).isDefined) {
				return None
			}

			val httpServer = new Server(bindAddress)
			val handler = createServletContextHandler("/api")
			httpServer.setHandler(handler)
			httpServer.start()
			this.serverRef.set(httpServer)
			Option(httpServer)
		}
	}

	/**
	 * 現在Webサーバーが動作中であれば、
	 * そのWebサーバーを停止させ、そのインスタンスを返します。
	 */
	def shutdown(): Option[Server] = {
		this.synchronized {
			val result = Option(this.serverRef.get)
			result.foreach {
				server => server.stop()
			}
			this.serverRef.set(null)
			result
		}
	}

	private def createServletContextHandler(contextPath: String): ServletContextHandler = {
		val result = new ServletContextHandler(ServletContextHandler.NO_SESSIONS)
		//コンテクストパスを設定する。
		result.setContextPath(contextPath)
		//REST API用のサーブレットを生成する。
		result.addServlet(new ServletHolder(new RestApiServlet), "/rest/*")
		result
	}
}
