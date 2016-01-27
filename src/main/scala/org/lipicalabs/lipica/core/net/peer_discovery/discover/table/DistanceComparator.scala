package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import java.util.Comparator

import org.lipicalabs.lipica.core.net.peer_discovery.NodeId

/**
 * 基準となるノードからの、
 * Kademlia的「距離」の長短によって
 * ノードを整序するための Comparator です。
 *
 * @param targetNodeId 距離を計算する基準となるノードID。
 *
  * Created by IntelliJ IDEA.
  * 2015/12/19 14:02
  * YANAGISAWA, Kentaro
  */
class DistanceComparator(private val targetNodeId: NodeId) extends Comparator[NodeEntry] {

	 override def compare(o1: NodeEntry, o2: NodeEntry): Int = {
		 val d1 = NodeEntry.distance(this.targetNodeId, o1.node.id)
		 val d2 = NodeEntry.distance(this.targetNodeId, o2.node.id)

		 //基準となるノードからの「距離」が短いほうが先になる。
		 if (d2 < d1) {
			 1
		 } else if (d1 < d2) {
			 -1
		 } else {
			 0
		 }
	 }

 }
