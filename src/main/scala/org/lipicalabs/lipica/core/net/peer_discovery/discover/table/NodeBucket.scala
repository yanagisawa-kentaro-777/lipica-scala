package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import java.util.Comparator

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * 一定数のノードの情報を格納するコンテナです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:54
 * YANAGISAWA, Kentaro
 */
class NodeBucket(val depth: Int) {

	private val nodesBuffer: mutable.Buffer[NodeEntry] = new ArrayBuffer[NodeEntry]
	def nodeCount: Int = this.nodesBuffer.size
	def nodes: Seq[NodeEntry] = this.nodesBuffer.toSeq

	/**
	 * 渡されたノードを、このバケットに追加しようとします。
	 */
	def addNode(e: NodeEntry): Option[NodeEntry] = {
		this.synchronized {
			if (!nodesBuffer.contains(e)) {
				if (KademliaOptions.BucketSize <= this.nodesBuffer.size) {
					//定員オーバー。
					return lastSeen
				} else {
					this.nodesBuffer.append(e)
				}
			}
			None
		}
	}

	private def lastSeen: Option[NodeEntry] = {
		nodes.sortWith((n1, n2) => new TimeComparator().compare(n1, n2) < 0).headOption
	}

	/**
	 * 渡されたノードの情報を、このバケットから削除します。
	 */
	def dropNode(entry: NodeEntry): Unit = {
		this.synchronized {
			for (idx <- this.nodesBuffer.indices) {
				val each = this.nodesBuffer(idx)
				if (each.entryId == entry.entryId) {
					nodesBuffer.remove(idx)
					return
				}
			}
		}
	}

	/**
	 * 渡されたノードの最終更新日時を更新します。
	 */
	def touch(entry: NodeEntry): Boolean = {
		this.synchronized {
			this.nodesBuffer.find(each => each.entryId == entry.entryId) match {
				case Some(found) =>
					found.touch()
					return true
				case None =>
					false
			}
		}
	}

}

/**
 * 最終更新日時が最近であるノード情報を
 * 先頭に近く整序する Comparator 実装です。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:02
 * YANAGISAWA, Kentaro
 */
class TimeComparator extends Comparator[NodeEntry] {

	override def compare(o1: NodeEntry, o2: NodeEntry): Int = {
		val t1 = o1.modified
		val t2 = o2.modified
		if (t1 < t2) {
			1
		} else if (t2 < t1) {
			-1
		} else {
			0
		}
	}

}