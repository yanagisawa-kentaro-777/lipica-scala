package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node}


/**
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:02
 * YANAGISAWA, Kentaro
 */
class NodeEntry private() {

	private var _ownerId: NodeId = null
	def ownerId: NodeId = this._ownerId

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

	def apply(ownerId: NodeId, n: Node): NodeEntry = {
		val result = new NodeEntry
		result._node = n
		result._ownerId = ownerId
		result._entryId = n.toString
		result._distance = distance(ownerId, n.id)
		result.touch()
		result
	}

	def distance(ownerId: NodeId, targetId: NodeId): Int ={
		//２個のバイト配列の256ビットダイジェストのXORを新たなバイト配列に格納する。
		val digest1 = ownerId.bytes.digest256.bytes
		val digest2 = targetId.bytes.digest256.bytes
		val xor = new Array[Byte](digest1.length)
		for (i <- xor.indices) {
			//等しいバイトはゼロになる。
			xor(i) = ((digest1(i) & 0xFF) ^ (digest2(i) & 0xFF)).toByte
		}

		var result = KademliaOptions.Bins
		//先頭から連続して一致している（つまり、xorがゼロになっている）バイト数は？
		val equalBytes = xor.takeWhile(each => each == 0).length
		//そのバイト数のビットの数だけ、距離を減らす。
		result -= (equalBytes * 8)
		//次の１バイトについて、先頭から一致するビット数を数える。
		val equalBits =
			if (equalBytes == xor.length) {
				//全部一致しているので残りはない。
				0
			} else {
				var count = 0
				var mask = 0x80
				val b = xor(equalBytes) & 0xFF
				//１ビットずつ右にシフトして、最上位ビットからの連続ゼロを数える。
				while ((mask != 0) && ((mask & b) == 0)) {
					count += 1
					mask = mask >>> 1
				}
				count
			}
		//発見されたビット数だけ距離を減らす。
		result -= equalBits
		result
	}
}
