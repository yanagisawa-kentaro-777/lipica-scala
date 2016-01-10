package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import org.lipicalabs.lipica.core.net.peer_discovery.Node

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:54
 * YANAGISAWA, Kentaro
 */
class NodeBucket(val depth: Int) {

	private val _nodes: mutable.Buffer[NodeEntry] = new ArrayBuffer[NodeEntry]
	def nodeCount: Int = this._nodes.size
	def nodes: Seq[NodeEntry] = this._nodes.toSeq

	def addNode(e: NodeEntry): Option[NodeEntry] = {
		this.synchronized {
			if (!_nodes.contains(e)) {
				if (KademliaOptions.BucketSize <= this._nodes.size) {
					//定員オーバー。
					return lastSeen
				} else {
					this._nodes.append(e)
				}
			}
			None
		}
	}

	private def lastSeen: Option[NodeEntry] = {
		nodes.sortWith((n1, n2) => new TimeComparator().compare(n1, n2) < 0).headOption
	}

	def dropNode(entry: NodeEntry): Unit = {
		this.synchronized {
			for (idx <- this._nodes.indices) {
				val each = this._nodes(idx)
				if (each.id == entry.id) {
					_nodes.remove(idx)
					return
				}
			}
		}
	}

	def touch(entry: NodeEntry): Boolean = {
		this.synchronized {
			this._nodes.find(each => each.id == entry.id) match {
				case Some(found) =>
					found.touch()
					return true
				case None =>
					false
			}
		}
	}

}
