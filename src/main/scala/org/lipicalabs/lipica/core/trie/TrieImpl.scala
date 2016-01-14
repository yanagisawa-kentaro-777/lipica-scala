package org.lipicalabs.lipica.core.trie

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.lipicalabs.lipica.core.utils._
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.slf4j.LoggerFactory
import org.lipicalabs.lipica.core.bytes_codec.RBACCodec

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Merkle-Patricia Treeの実装クラスです。
 *
 * @author YANAGISAWA, Kentaro
 * @since 2015/09/30
 */
class TrieImpl private[trie](_db: KeyValueDataSource, _root: ImmutableBytes) extends Trie {

	private[trie] def this(_db: KeyValueDataSource) = this(_db, DigestUtils.EmptyTrieHash)

	import TrieImpl._
	import org.lipicalabs.lipica.core.bytes_codec.NibbleCodec._

	/**
	 * 木構造のルート要素。
	 */
	private val rootRef = new AtomicReference[TrieNode](TrieNode.fromDigest(_root))
	override def root_=(node: TrieNode): TrieImpl = {
		if (node.isEmpty) {
			this.rootRef.set(TrieNode.emptyTrieNode)
		} else {
			this.rootRef.set(node)
		}
		this
	}
	def root_=(value: ImmutableBytes): TrieImpl = {
		this.root = TrieNode.fromDigest(value)
		this
	}
	def root: TrieNode = this.rootRef.get

	/**
	 * 最上位レベルのハッシュ値を計算して返します。
	 */
	override def rootHash: ImmutableBytes = this.rootRef.get.hash

	/**
	 * 前回のルート要素。（undo に利用する。）
	 */
	private val prevRootRef = new AtomicReference[TrieNode](this.rootRef.get)
	def prevRoot: TrieNode = this.prevRootRef.get

	/**
	 * 永続化機構のラッパー。
	 */
	private val dataStoreRef = new AtomicReference[TrieBackend](new TrieBackend(_db))
	def dataStore: TrieBackend = this.dataStoreRef.get
	def dataStore_=(v: TrieBackend): Unit = this.dataStoreRef.set(v)

	/**
	 * key 文字列に対応する値を取得して返します。
	 */
	def get(key: String): ImmutableBytes = get(ImmutableBytes(key.getBytes(StandardCharsets.UTF_8)))

	/**
	 * key に対応する値を取得して返します。
	 */
	override def get(key: ImmutableBytes): ImmutableBytes = {
		if (logger.isDebugEnabled) {
			logger.debug("<TrieImpl> Retrieving key [%s]".format(key.toHexString))
		}
		//終端記号がついた、１ニブル１バイトのバイト列に変換する。
		val convertedKey = binToNibbles(key)
		//ルートノード以下の全体を探索する。
		get(this.root, convertedKey)
	}

	@tailrec
	private def get(aNode: TrieNode, key: ImmutableBytes): ImmutableBytes = {
		if (key.isEmpty || aNode.isEmpty) {
			//キーが消費し尽くされているか、ノードに子孫がいない場合、そのノードの値を返す。
			return aNode.nodeValue
		}
		retrieveNode(aNode) match {
			case currentNode: ShortcutNode =>
				//２要素のショートカットノード。
				//このノードのキーを長ったらしい表現に戻す。
				val nodeKey = unpackToNibbles(currentNode.shortcutKey)
				//値を読み取る。
				if ((nodeKey.length <= key.length) && key.copyOfRange(0, nodeKey.length) == nodeKey) {
					//このノードのキーが、指定されたキーの接頭辞である。
					//子孫を再帰的に探索する。
					get(currentNode.childNode, key.copyOfRange(nodeKey.length, key.length))
				} else {
					//このノードは、指定されたキーの接頭辞ではない。
					//つまり、要求されたキーに対応する値は存在しない。
					ImmutableBytes.empty
				}
			case currentNode: RegularNode =>
				//このノードは、17要素の通常ノードである。
				//子孫をたどり、キーを１ニブル消費して探索を継続する。
				val child = currentNode.child(key(0))
				get(child, key.copyOfRange(1, key.length))
			case _ =>
				ImmutableBytes.empty
		}
	}

	/**
	 * key文字列 対応するエントリを削除します。
	 */
	def delete(key: String): Unit = {
		delete(ImmutableBytes(key.getBytes(StandardCharsets.UTF_8)))
	}

	/**
	 * 指定されたkeyに対応するエントリを削除します。
	 */
	override def delete(key: ImmutableBytes): Unit = {
		update(key, ImmutableBytes.empty)
	}

	/**
	 * key文字列 に対して値文字列を関連付けます。
	 */
	def update(key: String, value: String): Unit = {
		update(ImmutableBytes(key.getBytes(StandardCharsets.UTF_8)), ImmutableBytes(value.getBytes(StandardCharsets.UTF_8)))
	}

	/**
	 * key に対して値を関連付けます。
	 */
	override def update(key: ImmutableBytes, value: ImmutableBytes): Unit = {
		if (logger.isDebugEnabled) {
			logger.debug("<TrieImpl> Updating [%s] -> [%s]".format(key.toHexString, value.toHexString))
			logger.debug("<TrieImpl> Old root-hash: %s".format(rootHash.toHexString))
		}
		//終端記号がついた、１ニブル１バイトのバイト列に変換する。
		val nibbleKey = binToNibbles(key)
		val result = insertOrDelete(this.root, nibbleKey, value)
		//ルート要素を更新する。
		this.root = result
		if (logger.isDebugEnabled) {
			logger.debug("<TrieImpl> Updated [%s] -> [%s]".format(key.toHexString, value.toHexString))
			logger.debug("<TrieImpl> New root-hash: %s".format(rootHash.toHexString))
		}
	}

	private def insertOrDelete(node: TrieNode, key: ImmutableBytes, value: ImmutableBytes): TrieNode = {
		if (value.nonEmpty) {
			insert(node, key, ValueNode(value))
		} else {
			delete(node, key)
		}
	}

	/**
	 * キーに対応する値を登録します。
	 */
	private def insert(aNode: TrieNode, key: ImmutableBytes, valueNode: TrieNode): TrieNode = {
		if (key.isEmpty) {
			//終端記号すらない空バイト列ということは、
			//再帰的な呼び出しによってキーが消費しつくされたということ。
			//これ以上は処理する必要がない。
			return valueNode
		}
		val node = retrieveNode(aNode)
		if (node.isEmpty) {
			//親ノードが指定されていないので、新たな２要素ノードを作成して返す。
			val newNode = TrieNode(packNibbles(key), valueNode)
			return putToCache(newNode)
		}
		node match {
			case currentNode: ShortcutNode =>
				//２要素のショートカットノードである。
				//キーを長ったらしい表現に戻す。
				val nodeKey = unpackToNibbles(currentNode.shortcutKey)
				//キーの共通部分の長さをカウントする。
				val matchingLength = ByteUtils.matchingLength(key, nodeKey)
				val createdNode =
					if (matchingLength == nodeKey.length) {
						//既存ノードのキー全体が、新たなキーの接頭辞になっている。
						val remainingKeyPart = key.copyOfRange(matchingLength, key.length)
						//子孫を作る。
						insert(currentNode.childNode, remainingKeyPart, valueNode)
					} else {
						//既存ノードのキーの途中で分岐がある。
						//2要素のショートカットノードを、17要素の通常ノードに変換する。
						//従来の要素。
						val oldNode = insert(TrieNode.empty, nodeKey.copyOfRange(matchingLength + 1, nodeKey.length), currentNode.childNode)
						//追加された要素。
						val newNode = insert(TrieNode.empty, key.copyOfRange(matchingLength + 1, key.length), valueNode)
						//異なる最初のニブルに対応するノードを記録して、分岐させる。
						val scaledSlice = createRegularNodeSlice
						scaledSlice(nodeKey(matchingLength)) = oldNode
						scaledSlice(key(matchingLength)) = newNode
						putToCache(TrieNode(scaledSlice.toSeq))
					}
				if (matchingLength == 0) {
					//既存ノードのキーと新たなキーとの間に共通点はないので、
					//いま作成された通常ノードが、このノードの代替となる。
					createdNode
				} else {
					//このノードと今作られたノードとをつなぐノードを作成する。
					val bridgeNode = TrieNode(packNibbles(key.copyOfRange(0, matchingLength)), createdNode)
					putToCache(bridgeNode)
				}
			case currentNode: RegularNode =>
				//もともと17要素の通常ノードである。
				val newNode = copyRegularNode(currentNode)
				//普通にノードを更新して、保存する。
				newNode(key(0)) = insert(currentNode.child(key(0)), key.copyOfRange(1, key.length), valueNode)
				putToCache(TrieNode(newNode.toSeq))
			case other =>
				val s = if (other eq null) "null" else other.getClass.getSimpleName
				ErrorLogger.logger.warn("<Trie> Trie error: Node is %s".format(s))
				logger.warn("<Trie> Trie error: Node is %s".format(s))
				valueNode
		}
	}

	/**
	 * キーに対応するエントリーを削除します。
	 */
	private def delete(node: TrieNode, key: ImmutableBytes): TrieNode = {
		if (key.isEmpty || node.isEmpty) {
			//何もしない。
			return TrieNode.empty
		}
		retrieveNode(node) match {
			case currentNode: ShortcutNode =>
				//２要素のショートカットノードである。
				//長ったらしい表現に戻す。
				val packedKey = currentNode.shortcutKey
				val nodeKey = unpackToNibbles(packedKey)

				if (nodeKey == key) {
					//ぴたり一致。 これが削除対象である。
					TrieNode.empty
				} else if (nodeKey == key.copyOfRange(0, nodeKey.length)) {
					//このノードのキーが、削除すべきキーの接頭辞である。
					//再帰的に削除を試行する。削除した結果、新たにこのノードの直接の子になるべきノードが返ってくる。
					val deleteResult = delete(currentNode.childNode, key.copyOfRange(nodeKey.length, key.length))
					val newNode = retrieveNode(deleteResult) match {
							case newChild: ShortcutNode =>
								//削除で発生する跳躍をつなぐ。
								//この操作こそが、削除そのものである。
								val newKey = nodeKey ++ unpackToNibbles(newChild.shortcutKey)
								TrieNode(packNibbles(newKey), newChild.childNode)
							case _ =>
								TrieNode(packedKey, deleteResult)
						}
					putToCache(newNode)
				} else {
					//このノードは関係ない。
					node
				}
			case currentNode: RegularNode =>
				//もともと17要素の通常ノードである。
				val items = copyRegularNode(currentNode)
				//再帰的に削除する。
				val newChild = delete(items(key(0)), key.copyOfRange(1, key.length))
				//新たな子供をつなぎ直す。これが削除操作の本体である。
				items(key(0)) = newChild

				val idx = analyzeRegularNode(items)
				val newNode =
					if (idx == TERMINATOR.toInt) {
						//値以外は、すべてのキーが空白である。
						//すなわち、このノードには子はいない。
						//したがって、「終端記号 -> 値」のショートカットノードを生成する。
						TrieNode(packNibbles(ImmutableBytes.fromOneByte(TERMINATOR)), items(idx))
					} else if (0 <= idx) {
						//１ノードだけ子供がいて、このノードには値がない。
						//したがって、このノードと唯一の子供とを、ショートカットノードに変換できる。
							retrieveNode(items(idx)) match {
							case child: ShortcutNode =>
								val concat = ImmutableBytes.fromOneByte(idx.toByte) ++ unpackToNibbles(child.shortcutKey)
								TrieNode(packNibbles(concat), child.childNode)
							case _ =>
								TrieNode(packNibbles(ImmutableBytes.fromOneByte(idx.toByte)), items(idx))
						}
					} else {
						//２ノード以上子供がいるか、子どもと値がある。
						TrieNode(items.toSeq)
					}
				putToCache(newNode)
			case other =>
				if (logger.isDebugEnabled) {
					//存在しないノードの削除？
					val s = if (other eq null) "null" else other.getClass.getSimpleName
					logger.debug("<Trie> Trie error: Node is %s".format(s))
				}
				TrieNode.empty
		}
	}

	/**
	 * 17要素の通常ノードを分析して、
	 * (1) 値のみを持ち、子ノードを持たない場合。
	 * (2) １個の子ノードのみを持ち、それ以外に子ノードも値も持たない場合。
	 * (3) 上記のいずれでもない場合。（つまり、複数の子ノードを持つか、子ノードも値も持っている場合。）
	 * を判別して、
	 * (1) もしくは (2) の場合には、0 - 15 もしくは 16（TERMINATOR）の値を返し、
	 * (3) の場合には負の値を返します。
	 */
	private def analyzeRegularNode(node: Array[TrieNode]): Int = {
		var idx = -1
		(0 until TrieNode.RegularSize).foreach {
			i => {
				if (node(i) != TrieNode.empty) {
					if (idx == -1) {
						idx = i
					} else {
						idx = -2
					}
				}
			}
		}
		idx
	}

	private def retrieveNode(node: TrieNode): TrieNode = {
		if (!node.isDigestNode) {
			return node
		}
		if (node.isEmpty) {
			TrieNode.empty
		} else if (node.hash == DigestUtils.EmptyTrieHash) {
			TrieNode.empty
		} else {
			//対応する値を引いて返す。
			TrieNode(this.dataStore.get(node.hash).nodeValue)
		}
	}

	private def putToCache(node: TrieNode): TrieNode = {
		this.dataStore.put(convertNodeToValue(node)) match {
			case Left(v) =>
				//値がそのままである。
				node
			case Right(digest) =>
				//長かったので、ハッシュ値が返ってきたということ。
				TrieNode.fromDigest(digest)
		}
	}

	override def sync(): Unit = {
		this.dataStore.commit()
		this.prevRootRef.set(root)
	}

	override def undo(): Unit = {
		this.dataStore.undo()
		this.rootRef.set(prevRoot)
	}

	override def validate: Boolean = Option(this.dataStore.get(rootHash)).isDefined

	/**
	 * このTrieに属するすべての要素を、キャッシュから削除します。
	 */
	def cleanCache(): Unit = {
		val startTime = System.currentTimeMillis

		val collectAction = new CollectFullSetOfNodes
		this.scanTree(this.rootHash, collectAction)
		val collectedHashes = collectAction.getCollectedHashes

		val cachedNodes = this.dataStore.getNodes
		val toRemoveSet = new mutable.HashSet[ImmutableBytes]
		for (key <- cachedNodes.keySet) {
			if (!collectedHashes.contains(key)) {
				toRemoveSet.add(key)
			}
		}
		for (key <- toRemoveSet) {
			this.dataStore.delete(key)
			if (logger.isTraceEnabled) {
				logger.trace("<TrieImpl> Garbage collected node: [%s]".format(key.toHexString))
			}
		}
		logger.info("<TrieImpl> Garbage collected node list, size: [%,d]".format(toRemoveSet.size))
		logger.info("<TrieImpl> Garbage collection time: [%,d ms]".format(System.currentTimeMillis - startTime))
	}

	def copy: TrieImpl = {
		val another = new TrieImpl(this.dataStore.dataSource, rootHash)
		this.dataStore.getNodes.foreach {
			each => another.dataStore.privatePut(each._1, each._2)
		}
		another
	}

	private def scanTree(hash: ImmutableBytes, action: ScanAction): Unit = {
		val node = this.dataStore.get(hash)
		if (node.nodeValue.isSeq) {
			val siblings = node.nodeValue.asSeq
			if (siblings.size == TrieNode.ShortcutSize) {
				val value = Value.fromObject(siblings(1))
				if (value.isHashCode) scanTree(value.asBytes, action)
			} else {
				(0 until TrieNode.RegularSize).foreach {i => {
					val value = Value.fromObject(siblings(i))
					if (value.isHashCode) scanTree(value.asBytes, action)
				}}
			}
			action.doOnNode(hash, node.nodeValue)
		}
	}

	/**
	 * このTrieの中身に、渡されたバイト列の中身を充填します。
	 * @param data 符号化されたバイト列。
	 */
	def deserialize(data: ImmutableBytes): Unit = {
		RBACCodec.Decoder.decode(data) match {
			case Right(result) =>
				val keys = result.items.head.bytes
				val valuesSeq = result.items(1).items.map(_.bytes)
				val encodedRoot = result.items(2).bytes

				valuesSeq.indices.foreach {i => {
					val encodedValue = valuesSeq(i)
					val key = new Array[Byte](32)
					val value = Value.fromEncodedBytes(encodedValue).decode
					keys.copyTo(i * 32, key, 0, 32)

					this.dataStore.put(ImmutableBytes(key), value)
				}}
				this.root = TrieNode(Value.fromEncodedBytes(encodedRoot).decode)
			case Left(e) =>
				ErrorLogger.logger.warn("<TrieImpl> Deserialization error.", e)
				logger.warn("<TrieImpl> Deserialization error.", e)
		}
	}

	/**
	 * このTrieの中身をバイト列に符号化して返します。
	 * @return 符号化されたバイト列。
	 */
	def serialize: ImmutableBytes = {
		val nodes = this.dataStore.getNodes
		val encodedKeys = nodes.keys.foldLeft(Array.emptyByteArray)((accum, each) => accum ++ each.toByteArray)
		val encodedValues = nodes.values.map(each => RBACCodec.Encoder.encode(each.nodeValue.value))
		val encodedRoot = RBACCodec.Encoder.encode(this.root.hash)
		RBACCodec.Encoder.encode(Seq(encodedKeys, encodedValues, encodedRoot))
	}

	/**
	 * このTrieの中身を文字列化して返します。
	 * @return human readable な文字列。
	 */
	override def dumpToString: String = {
		val traceAction = new TraceAllNodes
		this.scanTree(this.rootHash, traceAction)

		val rootString = "root: %s => %s\n".format(rootHash.toHexString, this.root.toString)
		rootString + traceAction.getOutput
	}

	override def equals(o: Any): Boolean = {
		o match {
			case another: Trie => this.rootHash == another.rootHash
			case _ => false
		}
	}
}

object TrieImpl {
	private val logger = LoggerFactory.getLogger("trie")

	def newInstance: TrieImpl = new TrieImpl(null)

	def newInstance(ds: KeyValueDataSource): TrieImpl = new TrieImpl(ds)

	@tailrec
	def computeHash(obj: Either[ImmutableBytes, Value]): ImmutableBytes = {
		obj match {
			case null =>
				DigestUtils.EmptyTrieHash
			case Left(bytes) =>
				if (bytes.isEmpty) {
					DigestUtils.EmptyTrieHash
				} else {
					//バイト配列である場合には、計算されたハッシュ値であるとみなす。
					bytes
				}
			case Right(value) =>
				if (value.isBytes) {
					computeHash(Left(value.asBytes))
				} else {
					value.hash
				}
		}
	}

	def convertNodeToValue(aNode: TrieNode): Value = {
		aNode match {
			case node: ShortcutNode =>
				Value.fromObject(Seq(Value.fromObject(node.shortcutKey), convertNodeToValue(node.childNode)))
			case node: RegularNode =>
				Value.fromObject(node.children.map(each => convertNodeToValue(each)))
			case node: TrieNode =>
				Value.fromObject(node.nodeValue)
		}
	}

	private def createRegularNodeSlice: Array[TrieNode] = {
		(0 until TrieNode.RegularSize).map(_ => TrieNode.empty).toArray
	}

	/**
	 * １７要素ノードの要素を、可変の配列に変換する。
	 */
	private def copyRegularNode(node: RegularNode): Array[TrieNode] = {
		(0 until TrieNode.RegularSize).map(i => Option(node.child(i)).getOrElse(TrieNode.empty)).toArray
	}


}


trait TrieNode {
	def isDigestNode: Boolean
	def isEmpty: Boolean
	def isShortcutNode: Boolean
	def isRegularNode: Boolean
	def nodeValue: ImmutableBytes
	def hash: ImmutableBytes
}

object TrieNode {
	val ShortcutSize = 2
	val RegularSize = 17

	val emptyTrieNode = new DigestNode(DigestUtils.EmptyTrieHash)
	val empty = new DigestNode(ImmutableBytes.empty)
	def fromDigest(hash: ImmutableBytes): DigestNode = {
		if (hash.isEmpty) {
			empty
		} else {
			new DigestNode(hash)
		}
	}
	def apply(value: Value): TrieNode = {
		if (value.isSeq && value.length == ShortcutSize) {
			apply(value.get(0).get.asBytes, TrieNode(value.get(1).get))
		} else if (value.isSeq && value.length == RegularSize) {
			apply(value.asSeq.map(each => apply(Value.fromObject(each))))
		} else if (value.isBytes && value.asBytes.isEmpty) {
			TrieNode.empty
		} else {
			TrieNode.fromDigest(value.asBytes)
		}
	}

	def apply(key: ImmutableBytes, child: TrieNode): ShortcutNode = new ShortcutNode(key, child)

	def apply(children: Seq[TrieNode]): RegularNode = new RegularNode(children)

}

class ShortcutNode(val shortcutKey: ImmutableBytes, val childNode: TrieNode) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = false
	override val isShortcutNode: Boolean = true
	override val isRegularNode: Boolean = false
	override def hash = TrieImpl.computeHash(Right(TrieImpl.convertNodeToValue(this)))

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
	override def hash = TrieImpl.computeHash(Right(TrieImpl.convertNodeToValue(this)))
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

	override def hash: ImmutableBytes = TrieImpl.computeHash(Left(this.nodeValue))
}

object ValueNode {
	def apply(v: ImmutableBytes): ValueNode = new ValueNode(v)
}

class DigestNode(override val hash: ImmutableBytes) extends TrieNode {
	override val isEmpty: Boolean = this.hash.isEmpty
	override val isDigestNode: Boolean = true
	override val isShortcutNode: Boolean = false
	override val isRegularNode: Boolean = false
	override val nodeValue: ImmutableBytes = this.hash
	override def equals(o: Any): Boolean = {
		try {
			this.hash == o.asInstanceOf[DigestNode].hash
		} catch {
			case any: Throwable => false
		}
	}
}

