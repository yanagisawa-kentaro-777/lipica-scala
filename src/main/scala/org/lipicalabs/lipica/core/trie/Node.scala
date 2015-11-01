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
//
//object EmptyNode extends Node {
//	override val nodeValue = Value.empty
//	override val value = Value.empty
//	override val isEmpty = true
//}
//
//class ShortcutNode(val key: Array[Byte], override val value: Value) extends Node {
//	override def nodeValue = Value.fromObject(Seq(key, value))
//	override val isEmpty = false
//	def updated(v: Value): ShortcutNode = {
//		new ShortcutNode(this.key, v)
//	}
//	def updated(bytes: Array[Byte]): ShortcutNode = {
//		updated(Value.fromObject(bytes))
//	}
//}
//
//object ShortcutNode {
//	def apply(key: Array[Byte], value: Value): ShortcutNode = new ShortcutNode(key, value)
//	def apply(key: Array[Byte], value: Array[Byte]): ShortcutNode = new ShortcutNode(key, Value.fromObject(value))
//}
//
//class RegularNode(val children: IndexedSeq[Node], override val value: Value) extends Node {
//	override def nodeValue = {
//		val seq: Seq[Value] = children.map(_.nodeValue).toSeq :+ value
//		Value.fromObject(seq)
//	}
//	override val isEmpty = false
//
//	/**
//	 * 0 - 15 もしくは 16（TERMINATOR）の添字に応じて、
//	 * 子ノードもしくは値を返します。
//	 */
//	def apply(idx: Int): Either[Node, Value] = {
//		if (idx < this.children.size) {
//			Left(this.children(idx))
//		} else if (idx == this.children.size) {
//			Right(this.value)
//		} else {
//			throw new RuntimeException
//		}
//	}
//
//	/**
//	 * 0 - 15 もしくは 16（TERMINATOR）の添字に応じて、
//	 * 子ノードもしくは値を更新して返します。
//	 */
//	def updated(idx: Int, v: Either[Node, Value]): RegularNode = {
//		if (idx < this.children.size) {
//			new RegularNode(this.children.updated(idx, v.left.get), this.value)
//		} else if (idx == this.children.size) {
//			updated(v.right.get)
//		} else {
//			throw new RuntimeException
//		}
//	}
//	def updated(v: Value): RegularNode = {
//		new RegularNode(this.children, v)
//	}
//	def updated(bytes: Array[Byte]): RegularNode = {
//		updated(Value.fromObject(bytes))
//	}
//}
//
//object RegularNode {
//	def newInstance: RegularNode = new RegularNode((0 until 16).map(_ => EmptyNode), Value.empty)
//	def apply(children: Seq[Node], value: Value): RegularNode = new RegularNode(children.toIndexedSeq, value)
//}

