package org.lipicalabs.lipica.rest

import java.text.SimpleDateFormat

import org.lipicalabs.lipica.EntryPoint
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.facade.Lipica
import org.lipicalabs.lipica.core.facade.components.ComponentsMotherboard
import org.lipicalabs.lipica.utils.MiscUtils
import org.scalatra.ScalatraServlet
import org.scalatra._

/**
 * REST APIの呼び出しを処理する Servlet の実装です。
 *
 * （現状はまったくAPIらしくなく、テキストファイルで情報を返す挙動になっていますが。）
 *
 * Created by IntelliJ IDEA.
 * 2015/12/28 13:49
 * YANAGISAWA, Kentaro
 */
class RestApiServlet extends ScalatraServlet {

	/**
	 * 現在の自ノードの状態について、
	 * なんとなく知りたいことをテキストで返します。
	 * （こういうのはAPIと言わない。）
	 */
	get("/:apiVersion/node/status") {
		val lipica = Lipica.instance
		val componentsMotherboard = ComponentsMotherboard.instance
		val startedUnixMillis = lipica.startupTimestamp
		val startedTime = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z").format(startedUnixMillis)

		val blockchain = componentsMotherboard.blockchain
		val bestBlock = blockchain.bestBlock
		val totalDifficulty = blockchain.totalDifficulty

		val peersPool = componentsMotherboard.peersPool
		val nodeManager = componentsMotherboard.nodeManager
		val syncManager = componentsMotherboard.syncManager

		val bannedPeers = peersPool.bannedPeersMap

		val threads = MiscUtils.allThreads

		val body = ("NodeId=%s\n" +
				"ExternalAddress=%s\nBindAddress=%s\nStartedTime=%s\n\n" +
				"BestBlock=[%,d %s]\nTD=%,d\n\n" +
				"ProcessingBlock=%s\n\n" +
				"SyncState=%s\nSyncHashes=%,d\nSyncBlocks=%,d\nTD Range:\n\t%,d\n\t%,d\n\n" +
				"Active Peers:%,d\n%s\n\n" + "Banned Peers:%,d\n%s\n\n" + "Pending Peers:%,d\n\n" +
				"NumNodeHandlers:%,d\nNumNodesInTable:%,d\n\n" +
				"Threads: %,d\n\n").format(
			NodeProperties.instance.nodeId,
			NodeProperties.instance.externalAddress,
			NodeProperties.instance.bindAddress,
			startedTime,

			bestBlock.blockNumber, bestBlock.hash.toShortString,
			totalDifficulty,
			blockchain.processingBlockOption.map(_.summaryString(short = true)).getOrElse("None"),

			syncManager.state.name,
			syncManager.queue.hashStoreSize,
			syncManager.queue.blockQueueSize,
			syncManager.lowerUsefulDifficulty,
			syncManager.highestKnownDifficulty,

			peersPool.activeCount,
			peersPool.peers.map(each => {
				val hostAddress = each.node.address.getAddress.getHostAddress
				val hostName = each.node.address.getAddress.getCanonicalHostName
				"%s\t%s\t%d\t%s\tTD=%,d\t%s".format(
					each.nodeIdShort, hostAddress, each.node.address.getPort, each.syncStateSummaryAsString, each.totalDifficulty, hostName
				)
			}).mkString("\n"),
			bannedPeers.size,
			bannedPeers.map(entry => entry._1.toShortString + " -> " + entry._2).mkString("\n"),
			peersPool.pendingCount,

			nodeManager.numberOfKnownNodes,
			nodeManager.table.nodeCount,

			threads.length
		)
		status = 200
		Ok(body)
	}

	/**
	 * 現在の自ノードで動作するスレッドの一覧をテキストで返します。
	 * （こういうのはAPIと言わない。）
	 */
	get("/:apiVersion/node/status/threads") {
		val threads = MiscUtils.allThreads.toSeq.sortWith((t1, t2) => t1.getName.compareTo(t2.getName) < 0)
		val body = "Number of threads: %,d\n\n%s\n\n".format(threads.size, threads.map(_.getName).mkString("\n"))
		status = 200
		Ok(body)
	}

	/**
	 * 現在の自ノードのメモリ消費状況をテキストで返します。
	 * （こういうのはAPIと言わない。）
	 */
	get("/:apiVersion/node/status/memory") {
		val body = renderMemoryInfo
		status = 200
		Ok(body)
	}

	/**
	 * このプロセスで GC を実行します。
	 * （メモリリーク疑惑があった場合のデバッグ用。）
	 * （こういうのはAPIと言わない。）
	 */
	get("/:apiVersion/node/status/memory/gc") {
		val runtime = Runtime.getRuntime
		runtime.gc()

		val body = renderMemoryInfo
		status = 200
		Ok(body)
	}

	/**
	 * 自ノードの動作を終了させます。
	 * （ブラウザから簡単に実行できるように GET にしている。）
	 */
	get("/:apiVersion/node/stop") {
		val task = new Runnable {
			override def run() = {
				//このリクエストの処理が完了するように、しばらく待ってからシャットダウンする。
				Thread.sleep(3000L)
				EntryPoint.shutdown()
			}
		}
		new Thread(task).start()
		status = 200
		Ok("Shutting down...")
	}

	private def renderMemoryInfo: String = {
		val runtime = Runtime.getRuntime
		val totalMemory = runtime.totalMemory
		val usedMemory = totalMemory - runtime.freeMemory
		val maxMemory = runtime.totalMemory

		"Used memory\t%,d bytes\nTotal memory\t%,d bytes\nMax memory\t%,d bytes\n\n".format(
			usedMemory, totalMemory, maxMemory
		)
	}

}
