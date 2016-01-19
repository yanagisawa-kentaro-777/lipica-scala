package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

import scala.collection.mutable.ArrayBuffer

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 15:04
 * YANAGISAWA, Kentaro
 */
class NodeTable(val node: Node, includedHomeNode: Boolean) {

	def this(node: Node) = this(node, includedHomeNode = true)

	import NodeTable._

	private var _buckets: IndexedSeq[NodeBucket] = null
	def buckets: IndexedSeq[NodeBucket] = this._buckets

	private var _nodes: scala.collection.mutable.Buffer[NodeEntry] = null
	def nodes: Seq[NodeEntry] = this._nodes.toSeq

	def initialize(): Unit = {
		this.synchronized {
			this._nodes = new ArrayBuffer[NodeEntry]
			this._buckets = (0 until KademliaOptions.Bins).map(i => new NodeBucket(i)).toIndexedSeq
		}
	}

	def addNode(n: Node): Option[Node] = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this._buckets(getBucketId(e)).addNode(e) match {
				case Some(lastSeen) => Option(lastSeen.node)
				case None =>
					if (!this._nodes.contains(e)) {
						this._nodes.append(e)
					}
					None
			}
		}
	}

	def dropNode(n: Node): Unit = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this._buckets(getBucketId(e)).dropNode(e)
			this._nodes.remove(this._nodes.indexOf(e))
		}
	}

	def contains(n: Node): Boolean = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this._buckets.exists(bucket => bucket.nodes.contains(e))
		}
	}

	def touch(n: Node): Boolean = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this._buckets.exists(bucket => bucket.touch(e))
		}
	}

	def getBucketCount: Int = {
		this.synchronized {
			this._buckets.count(bucket => 0 < bucket.nodeCount)
		}
	}

	def getNodeCount: Int = {
		this.synchronized {
			this._nodes.size
		}
	}

	def getAllNodes: Seq[NodeEntry] = {
		this.synchronized {
			this._buckets.flatMap {
				bucket => bucket.nodes.filter(each => each.node != this.node)
			}
		}
	}

	def getClosestNodes(targetId: NodeId): Seq[Node] = {
		this.synchronized {
			val comparator = new DistanceComparator(targetId)
			getAllNodes.sortWith((e1, e2) => comparator.compare(e1, e2) < 0).take(KademliaOptions.BucketSize).map(_.node)
		}
	}

	initialize()
	if (includedHomeNode) {
		addNode(this.node)
	}
}

object NodeTable {
	def getBucketId(e: NodeEntry): Int = {
		val id = e.distance - 1
		id max 0
	}
}