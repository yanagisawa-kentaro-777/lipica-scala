package org.lipicalabs.lipica.core.trie

import java.util.concurrent.atomic.AtomicBoolean
import org.lipicalabs.lipica.core.utils.Value

/**
 * Merkle-Patricia Tree の１ノードを表すクラスです。
 */
trait Node {
	/**
	 * ノードのキーおよび値をまとめたValueオブジェクトを返します。
	 */
	def nodeValue: Value

	/**
	 * ノードが保持する値のみを表現するValueオブジェクトを返します。
	 */
	def value: Value

	def isEmpty: Boolean

}

class CachedNode(override val nodeValue: Value, _dirty: Boolean) extends Node {
	/**
	 * 永続化されていない更新があるか否か。
	 */
	private val isDirtyRef = new AtomicBoolean(_dirty)
	override val value: Value = nodeValue
	def isDirty = this.isDirtyRef.get
	def isDirty(value: Boolean): Node = {
		this.isDirtyRef.set(value)
		this
	}
	override def isEmpty: Boolean = this.nodeValue.isNull || (this.nodeValue.length == 0)
}

object CachedNode {
	def fromNode(node: Node): CachedNode = new CachedNode(node.nodeValue, _dirty = true)
}
