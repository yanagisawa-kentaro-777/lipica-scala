package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import java.util.Comparator

import org.lipicalabs.lipica.core.net.peer_discovery.NodeId

/**
  * Created by IntelliJ IDEA.
  * 2015/12/19 14:02
  * YANAGISAWA, Kentaro
  */
class DistanceComparator(private val targetId: NodeId) extends Comparator[NodeEntry] {

	 override def compare(o1: NodeEntry, o2: NodeEntry): Int = {
		 val d1 = NodeEntry.distance(this.targetId, o1.node.id)
		 val d2 = NodeEntry.distance(this.targetId, o2.node.id)

		 if (d2 < d1) {
			 1
		 } else if (d1 < d2) {
			 -1
		 } else {
			 0
		 }
	 }

 }
