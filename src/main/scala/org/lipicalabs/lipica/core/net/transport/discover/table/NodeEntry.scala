package org.lipicalabs.lipica.core.net.transport.discover.table

import org.lipicalabs.lipica.core.net.transport.Node
import org.lipicalabs.lipica.core.utils.ImmutableBytes

import scala.util.control.Breaks

/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:02
 * YANAGISAWA, Kentaro
 */
class NodeEntry private() {

	private var _ownerId: ImmutableBytes = null
	def ownerId: ImmutableBytes = this._ownerId

	private var _node: Node = null
	def node: Node = this._node

	private var _entryId: String = null
	def entryId: String = this._entryId
	def id: String = this.entryId

	private var _distance: Int = 0
	def distance: Int = this._distance

	private var _modified: Long = 0L
	def modified: Long = this._modified

	def touch(): Unit = {
		this._modified = System.currentTimeMillis
	}

	override def equals(o: Any): Boolean = {
		try {
			this.id == o.asInstanceOf[NodeEntry].id
		} catch {
			case any: Throwable => false
		}
	}

	override def hashCode: Int = this.node.hashCode

}

object NodeEntry {

	def apply(n: Node): NodeEntry = {
		val result = new NodeEntry
		result._node = n
		result._ownerId = n.id
		result._entryId = n.toString
		result._distance = distance(result.ownerId, n.id)
		result.touch()
		result
	}

	def apply(ownerId: ImmutableBytes, n: Node): NodeEntry = {
		val result = new NodeEntry
		result._node = n
		result._ownerId = ownerId
		result._entryId = n.toString
		result._distance = distance(ownerId, n.id)
		result.touch()
		result
	}

	def distance(ownerId: ImmutableBytes, targetId: ImmutableBytes): Int ={
		val h1 = targetId
		val h2 = ownerId

		val hash = new Array[Byte](h1.length min h2.length)
		for (i <- hash.indices) {
			hash(i) = ((h1(i) & 0xFF) ^ (h2(i) & 0xFF)).toByte
		}
		var d = KademliaOptions.Bins
		val outerBrk = new Breaks
		outerBrk.breakable {
			for (b <- hash) {
				if (b == 0) {
					d -= 8
				} else {
					var count = 0
					val brk = new Breaks
					brk.breakable {
						for (i <- 7 to 0 by -1) {
							val a: Boolean = (b & (1 << i)) == 0
							if (a) {
								count += 1
							} else {
								brk.break()
							}
						}
					}
					d -= count
					outerBrk.break()
				}
			}
		}
		//println(ownerId.toHexString + " & " + targetId.toHexString + " -> " + d)
		d
	}
}
