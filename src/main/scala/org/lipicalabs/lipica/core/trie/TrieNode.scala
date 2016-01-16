package org.lipicalabs.lipica.core.trie

import java.util.concurrent.atomic.AtomicReference

import org.lipicalabs.lipica.core.bytes_codec.RBACCodec
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.crypto.digest.{EmptyDigest, Digest256, DigestUtils, DigestValue}
import org.lipicalabs.lipica.core.utils.ImmutableBytes

/**
 * Trieのノードを表す trait です。
 *
 * @since 2016/01/16
 * @author YANAGISAWA, Kentaro
 */
sealed trait TrieNode {
	def isDigestNode: Boolean
	def isEmpty: Boolean
	def isShortcutNode: Boolean
	def isRegularNode: Boolean

	def nodeValue: ImmutableBytes
	def hash: DigestValue

	private val encodedBytesRef = new AtomicReference[ImmutableBytes](null)
	def toEncodedBytes: ImmutableBytes = {
		var result = this.encodedBytesRef.get
		if (result ne null) {
			return result
		}
		result = TrieNode.encode(this)
		this.encodedBytesRef.set(result)
		result
	}

	override def toString: String = {
		"%s (%s)".format(getClass.getSimpleName, nodeValue.toShortString)
	}
}

object TrieNode {
	val ShortcutSize = 2
	val RegularSize = 17

	val emptyTrieNode = new DigestNode(DigestUtils.EmptyTrieHash)

	def fromDigest(hash: DigestValue): TrieNode = {
		new DigestNode(hash)
	}

	def apply(key: ImmutableBytes, child: TrieNode): ShortcutNode = new ShortcutNode(key, child)

	def apply(children: Seq[TrieNode]): RegularNode = new RegularNode(children)

	private def encode(aNode: TrieNode): ImmutableBytes = {
		val encoder = RBACCodec.Encoder
		aNode match {
			case node: ShortcutNode =>
				encoder.encodeSeqOfByteArrays(Seq(encoder.encode(node.shortcutKey), encode(node.childNode)))
			case node: RegularNode =>
				encoder.encodeSeqOfByteArrays(node.children.map(each => encode(each)))
			case node: ValueNode =>
				encoder.encode(node.nodeValue)
			case node: DigestNode =>
				encoder.encode(node.hash.bytes)
			case EmptyNode =>
				encoder.encode(ImmutableBytes.empty)
		}
	}

	def decode(encodedBytes: ImmutableBytes): TrieNode = {
		if (encodedBytes.isEmpty) {
			EmptyNode
		} else {
			val decodedResult = RBACCodec.Decoder.decode(encodedBytes).right.get
			decode(decodedResult)
		}
	}

	private def decode(decodedResult: DecodedResult): TrieNode = {
		if (decodedResult.isSeq) {
			val items = decodedResult.items
			if (items.size == ShortcutSize) {
				//ショートカットノード。
				new ShortcutNode(items.head.bytes, decode(items(1)))
			} else {
				new RegularNode(items.map(each => decode(each)))
			}
		} else {
			val bytes = decodedResult.bytes
			if (bytes.isEmpty) {
				EmptyNode
			} else if (bytes.length == Digest256.NumberOfBytes) {
				//ここで、データ長が32バイトのValueNodeあった場合、
				//それを誤ってDigestNodeとして復元してしまう問題がある。
				//しかしそのように誤復元されたとしても、
				//ValueNodeにおいて問題になるnodeValueの値は狂わないので、
				//結果として問題ないのだ。
				new DigestNode(Digest256(bytes))
			} else {
				new ValueNode(bytes)
			}
		}
	}

	def calculateHash(node: TrieNode): DigestValue = {
		node match {
			case null =>
				DigestUtils.EmptyTrieHash
			case node: ShortcutNode =>
				encode(node).digest256
			case node: RegularNode =>
				encode(node).digest256
			case node: ValueNode =>
				node.nodeValue.digest256
			case node: DigestNode =>
				node.hash
			case EmptyNode => EmptyDigest
		}
	}
}

object EmptyNode extends TrieNode {
	override val isEmpty = true
	override val isDigestNode = false
	override val isRegularNode = false
	override val isShortcutNode = false
	override val nodeValue = ImmutableBytes.empty
	override val hash = EmptyDigest
}

class ShortcutNode(val shortcutKey: ImmutableBytes, val childNode: TrieNode) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = false
	override val isShortcutNode: Boolean = true
	override val isRegularNode: Boolean = false
	override def hash = TrieNode.calculateHash(this)

	override def nodeValue: ImmutableBytes = this.childNode.nodeValue

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[ShortcutNode]
			(this.shortcutKey == another.shortcutKey) && (this.childNode == another.childNode)
		} catch {
			case any: Throwable => false
		}
	}
}

class RegularNode(val children: Seq[TrieNode]) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = false
	override val isShortcutNode: Boolean = false
	override val isRegularNode: Boolean = true
	override def hash = TrieNode.calculateHash(this)
	override def nodeValue: ImmutableBytes = this.children(16).nodeValue
	def child(idx: Int): TrieNode = this.children(idx)

	override def equals(o: Any): Boolean = {
		try {
			val another = o.asInstanceOf[RegularNode]
			for (i <- 0 until TrieNode.RegularSize) {
				if (this.child(i) != another.child(i)) {
					return false
				}
			}
			true
		} catch {
			case any: Throwable => false
		}
	}
}

class ValueNode(override val nodeValue: ImmutableBytes) extends TrieNode {
	override def isDigestNode: Boolean = false

	override def isRegularNode: Boolean = false
	override def isShortcutNode: Boolean = false
	override def isEmpty: Boolean = false

	override def hash: DigestValue = TrieNode.calculateHash(this)

	override def equals(o: Any): Boolean = {
		try {
			this.nodeValue == o.asInstanceOf[ValueNode].nodeValue
		} catch {
			case any: Throwable => false
		}
	}
}

object ValueNode {
	def apply(v: ImmutableBytes): ValueNode = new ValueNode(v)
}

class DigestNode(override val hash: DigestValue) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = true
	override val isShortcutNode: Boolean = false
	override val isRegularNode: Boolean = false
	override val nodeValue: ImmutableBytes = this.hash.bytes
	override def equals(o: Any): Boolean = {
		try {
			this.hash == o.asInstanceOf[DigestNode].hash
		} catch {
			case any: Throwable => false
		}
	}
}

