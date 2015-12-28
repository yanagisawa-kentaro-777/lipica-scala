package org.lipicalabs.lipica.core.net.transport.discover.table

import java.util.Comparator

import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
  * Created by IntelliJ IDEA.
  * 2015/12/19 14:02
  * YANAGISAWA, Kentaro
  */
class DistanceComparator(private val targetId: ImmutableBytes) extends Comparator[NodeEntry] {

	 override def compare(o1: NodeEntry, o2: NodeEntry): Int = {
		 val d1 = NodeEntry.distance(this.targetId, o1.node.id)
		 val d2 = NodeEntry.distance(this.targetId, o2.node.id)

		 if (d2 < d1) {
			 println(o2.node.id + " < " + o1.node.id)//TODO
			 1
		 } else if (d1 < d2) {
			 println(o1.node.id + " < " + o2.node.id) //TODO
			 -1
		 } else {
			 println(o1.node.id + " == " + o2.node.id) //TODO
			 0
		 }
	 }

 }
