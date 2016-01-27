package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node}

import scala.collection.mutable.ArrayBuffer

/**
 * Kademlia的なピアディスカバリーにおいて、
 * 発見されたノードの情報を格納するバケットを取りまとめるコンテナクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 15:04
 * YANAGISAWA, Kentaro
 */
class NodeTable(val node: Node, includedHomeNode: Boolean) {

	def this(node: Node) = this(node, includedHomeNode = true)

	import NodeTable._

	/**
	 * このテーブルが保持するバケット群。
	 */
	private val buckets: IndexedSeq[NodeBucket] = (0 until KademliaOptions.Bins).map(i => new NodeBucket(i))

	/**
	 * このテーブルが保持するバケットに含まれるノード群。
	 */
	private val nodesBuffer = new ArrayBuffer[NodeEntry]
	def nodes: Seq[NodeEntry] = this.nodesBuffer.toSeq

	/**
	 * 渡されたノードを、然るべきバケットに追加しようとします。
	 */
	def addNode(n: Node): Option[Node] = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			val bucketId = calculateBucketId(e)
			this.buckets(bucketId).addNode(e) match {
				case Some(lastSeen) => Option(lastSeen.node)
				case None =>
					if (!this.nodesBuffer.contains(e)) {
						this.nodesBuffer.append(e)
					}
					None
			}
		}
	}

	/**
	 * 渡されたノードを、然るべきバケットから削除しようとします。
	 */
	def dropNode(n: Node): Unit = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			val bucketId = calculateBucketId(e)
			this.buckets(bucketId).dropNode(e)
			this.nodesBuffer.remove(this.nodesBuffer.indexOf(e))
		}
	}

	/**
	 * 渡されたノードが、このオブジェクトが管理するバケットに含まれているか否かを返します。
	 */
	def contains(n: Node): Boolean = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this.buckets.exists(bucket => bucket.nodes.contains(e))
		}
	}

	/**
	 * 渡されたノードの最終更新日時を更新しようとします。
	 */
	def touch(n: Node): Boolean = {
		this.synchronized {
			val e = NodeEntry(this.node.id, n)
			this.buckets.exists(bucket => bucket.touch(e))
		}
	}

	def bucketCount: Int = {
		this.synchronized {
			this.buckets.count(bucket => 0 < bucket.nodeCount)
		}
	}

	def nodeCount: Int = {
		this.synchronized {
			this.nodesBuffer.size
		}
	}

	def allNodes: Seq[NodeEntry] = {
		this.synchronized {
			this.buckets.flatMap {
				bucket => bucket.nodes.filter(each => each.node != this.node)
			}
		}
	}

	def closedNodes(targetId: NodeId): Seq[Node] = {
		this.synchronized {
			val comparator = new DistanceComparator(targetId)
			allNodes.sortWith((e1, e2) => comparator.compare(e1, e2) < 0).take(KademliaOptions.BucketSize).map(_.node)
		}
	}

	if (includedHomeNode) {
		addNode(this.node)
	}
}

object NodeTable {

	/**
	 * 渡されたノードの情報を格納すべきバケットのIDを返します。
	 */
	private def calculateBucketId(e: NodeEntry): Int = {
		val id = e.distance - 1
		id max 0
	}
}