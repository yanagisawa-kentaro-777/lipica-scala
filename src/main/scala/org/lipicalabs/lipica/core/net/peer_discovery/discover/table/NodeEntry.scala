package org.lipicalabs.lipica.core.net.peer_discovery.discover.table

import java.util.concurrent.atomic.AtomicLong

import org.lipicalabs.lipica.core.net.peer_discovery.{NodeId, Node}


/**
 * Kademlia風のピアディスカバリーにおいて
 * 管理されるノードの情報をまとめたクラスです。
 *
 * Created by IntelliJ IDEA.
 * 2015/12/19 14:02
 * YANAGISAWA, Kentaro
 */
class NodeEntry private(val ownerId: NodeId, val node: Node, val entryId: String, val distance: Int) {

	private val modifiedTimeRef = new AtomicLong(0L)
	def modified: Long = this.modifiedTimeRef.get

	def touch(): Unit = {
		this.modifiedTimeRef.set(System.currentTimeMillis)
	}

	override def equals(o: Any): Boolean = {
		try {
			this.entryId == o.asInstanceOf[NodeEntry].entryId
		} catch {
			case any: Throwable => false
		}
	}

	override def hashCode: Int = this.node.hashCode

}

object NodeEntry {

	/**
	 * Kademliaで管理されるノード情報のインスタンスを生成します。
	 *
	 * @param ownerId 距離を計測する基準となるノードのID。
	 * @param n このエントリーで管理されるノード。
	 */
	def apply(ownerId: NodeId, n: Node): NodeEntry = {
		val result = new NodeEntry(ownerId, n, n.toCanonicalString, distance(ownerId, n.id))
		result.touch()
		result
	}

	def distance(node1: NodeId, node2: NodeId): Int ={
		//２個のバイト配列の256ビットダイジェストのXORを新たなバイト配列に格納する。
		val digest1 = node1.bytes.digest256.bytes
		val digest2 = node2.bytes.digest256.bytes
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
