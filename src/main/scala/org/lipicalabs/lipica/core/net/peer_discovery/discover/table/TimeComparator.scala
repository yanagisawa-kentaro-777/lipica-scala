package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import java.util.Comparator

/**
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
