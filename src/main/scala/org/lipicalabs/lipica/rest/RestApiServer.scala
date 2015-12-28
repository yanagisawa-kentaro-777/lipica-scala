package org.lipicalabs.lipica.rest

import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicReference

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.{ServletHolder, ServletContextHandler}

/**
 * Created by IntelliJ IDEA.
 * 2015/12/28 13:46
 * YANAGISAWA, Kentaro
 */
object RestApiServer {

	private val serverRef = new AtomicReference[Server](null)


	def startup(bindAddress: InetSocketAddress): Unit = {
		val httpServer = new Server(bindAddress)
		val handler = createServletContextHandler("/api/")
		httpServer.setHandler(handler)
		httpServer.start()
		this.serverRef.set(httpServer)
	}

	def shutdown(): Unit = {
		Option(this.serverRef.get).foreach {
			server => server.stop()
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
