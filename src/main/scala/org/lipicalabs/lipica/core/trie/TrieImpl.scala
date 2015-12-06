package org.lipicalabs.lipica.core.trie

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.lipicalabs.lipica.core.utils._
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db.datasource.KeyValueDataSource
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Merkle-Patricia Treeの実装クラスです。
 *
 * @author YANAGISAWA, Kentaro
 * @since 2015/09/30
 */
class TrieImpl private[trie](_db: KeyValueDataSource, _root: ImmutableBytes) extends Trie {

	import TrieImpl._
	import CompactEncoder._

	def this(_db: KeyValueDataSource) = this(_db, DigestUtils.EmptyTrieHash)

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
	private val cacheRef = new AtomicReference[Cache](new Cache(_db))
	def cache: Cache = this.cacheRef.get
	def cache_=(v: Cache): Unit = {
		this.cacheRef.set(v)
	}

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
			//キーが消費し尽くされているか、ノードに子孫がいない場合、そのノードを返す。
			return aNode.value.asBytes
		}
		val currentNode = retrieveNode2(aNode)
		if (currentNode.isShortcutNode) {
			//このノードのキーを長ったらしい表現に戻す。
			val k = unpackToNibbles(currentNode.child(0).value.asBytes)
			//値を読み取る。
			val v = currentNode.child(1).value
			if ((k.length <= key.length) && key.copyOfRange(0, k.length) == k) {
				if (key.length == k.length) {
					//完全一致！
					return v.asBytes
				}
				//このノードのキーが、指定されたキーの接頭辞である。
				//子孫を再帰的に探索する。
				get(TrieNode(v), key.copyOfRange(k.length, key.length))
			} else {
				//このノードは、指定されたキーの接頭辞ではない。
				//つまり、要求されたキーに対応する値は存在しない。
				ImmutableBytes.empty
			}
		} else if (currentNode.isRegularNode) {
			//このノードは、17要素の通常ノードである。
			//子孫をたどり、キーを１ニブル消費して探索を継続する。
			val child = currentNode.child(key(0)).value
			get(TrieNode(child), key.copyOfRange(1, key.length))
		} else {
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
			insert(node, key, Value.fromObject(value))
		} else {
			delete(node, key)
		}
	}

	/**
	 * キーに対応する値を登録します。
	 */
	private def insert(aNode: TrieNode, key: ImmutableBytes, value: Value): TrieNode = {
		if (key.isEmpty) {
			//終端記号すらない空バイト列ということは、
			//再帰的な呼び出しによってキーが消費しつくされたということ。
			//これ以上は処理する必要がない。
			return TrieNode(value)
		}
		val currentNode = retrieveNode(aNode)
		if (TrieNode(currentNode).isEmpty) {
			//親ノードが指定されていないので、新たな２要素ノードを作成して返す。
			val newNode = Seq(packNibbles(key), value)
			return putToCache(TrieNode(Value.fromObject(newNode)))
		}
		if (currentNode.length == PAIR_SIZE) {
			//２要素のショートカットノードである。
			val packedKey = currentNode.get(0).get.asBytes
			//キーを長ったらしい表現に戻す。
			val k = unpackToNibbles(packedKey)
			//値を取得する。
			val v = currentNode.get(1).get
			val matchingLength = ByteUtils.matchingLength(key, k)
			val createdNode =
				if (matchingLength == k.length) {
					//既存ノードのキー全体が、新たなキーの接頭辞になっている。
					val remainingKeyPart = key.copyOfRange(matchingLength, key.length)
					//子孫を作る。
					insert(TrieNode(v), remainingKeyPart, value)
				} else {
					//既存ノードのキーの途中で分岐がある。
					//2要素のショートカットノードを、17要素の通常ノードに変換する。
					//従来の要素。
					val oldNode = insert(TrieNode.empty, k.copyOfRange(matchingLength + 1, k.length), v).value
					//追加された要素。
					val newNode = insert(TrieNode.empty, key.copyOfRange(matchingLength + 1, key.length), value).value
					//異なる最初のニブルに対応するノードを記録して、分岐させる。
					val scaledSlice = emptyValueSlice(LIST_SIZE)
					scaledSlice(k(matchingLength)) = oldNode
					scaledSlice(key(matchingLength)) = newNode
					putToCache(TrieNode(Value.fromObject(scaledSlice.toSeq)))
				}
			if (matchingLength == 0) {
				//既存ノードのキーと新たなキーとの間に共通点はないので、
				//いま作成された通常ノードが、このノードの代替となる。
				createdNode
			} else {
				//このノードと今作られたノードとをつなぐノードを作成する。
				val bridgeNode = Seq(packNibbles(key.copyOfRange(0, matchingLength)), createdNode.value)
				putToCache(TrieNode(Value.fromObject(bridgeNode)))
			}
		} else {
			//もともと17要素の通常ノードである。
			val newNode = copyNode(TrieNode(currentNode))
			//普通にノードを更新して、保存する。
			newNode(key(0)) = insert(TrieNode(currentNode.get(key(0)).get), key.copyOfRange(1, key.length), value).value
			putToCache(TrieNode(Value.fromObject(newNode.toSeq)))
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
		val currentNode = retrieveNode(node)
		if (currentNode.length == PAIR_SIZE) {
			//２要素のショートカットノードである。
			//長ったらしい表現に戻す。
			val packedKey = currentNode.get(0).get.asBytes
			val k = unpackToNibbles(packedKey)

			if (k == key) {
				//ぴたり一致。 これが削除対象である。
				TrieNode.empty
			} else if (k == key.copyOfRange(0, k.length)) {
				//このノードのキーが、削除すべきキーの接頭辞である。
				//再帰的に削除を試行する。削除した結果、新たにこのノードの直接の子になるべきノードが返ってくる。
				val deleteResult = delete(TrieNode(currentNode.get(1).get), key.copyOfRange(k.length, key.length))
				val newChild = retrieveNode(deleteResult)
				val newNode =
					if (newChild.length == PAIR_SIZE) {
						//削除で発生する跳躍をつなぐ。
						//この操作こそが、削除そのものである。
						val newKey = k ++ unpackToNibbles(newChild.get(0).get.asBytes)
						Seq(packNibbles(newKey), newChild.get(1).get)
					} else {
						Seq(packedKey, deleteResult.value)
					}
				putToCache(TrieNode(Value.fromObject(newNode)))
			} else {
				//このノードは関係ない。
				node
			}
		} else {
			//もともと17要素の通常ノードである。
			val items = copyNode(TrieNode(currentNode))
			//再帰的に削除する。
			val newChild = delete(TrieNode(items(key(0))), key.copyOfRange(1, key.length)).value
			//新たな子供をつなぎ直す。これが削除操作の本体である。
			items(key(0)) = newChild

			val idx = analyzeRegularNode(items)
			val newNode =
				if (idx == TERMINATOR.toInt) {
					//値以外は、すべてのキーが空白である。
					//すなわち、このノードには子はいない。
					//したがって、「終端記号 -> 値」のショートカットノードを生成する。
					Seq(packNibbles(ImmutableBytes.fromOneByte(TERMINATOR)), items(idx))
				} else if (0 <= idx) {
					//１ノードだけ子供がいて、このノードには値がない。
					//したがって、このノードと唯一の子供とを、ショートカットノードに変換できる。
					val child = retrieveNode(TrieNode(items(idx)))
					if (child.length == PAIR_SIZE) {
						val concat = ImmutableBytes.fromOneByte(idx.toByte) ++ unpackToNibbles(child.get(0).get.asBytes)
						Seq(packNibbles(concat), child.get(1).get)
					} else if (child.length == LIST_SIZE) {
						Seq(packNibbles(ImmutableBytes.fromOneByte(idx.toByte)), items(idx))
					}
				} else {
					//２ノード以上子供がいるか、子どもと値がある。
					items.toSeq
				}
			putToCache(TrieNode(Value.fromObject(newNode)))
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
	private def analyzeRegularNode(node: Array[Value]): Int = {
		var idx = -1
		(0 until LIST_SIZE).foreach {
			i => {
				if (node(i) != Value.empty) {
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

	private def retrieveNode(node: TrieNode): Value = {
		if (!node.isDigestNode) {
			return node.value
		}
		if (node.isEmpty) {
			Value.empty
		} else if (node.hash == DigestUtils.EmptyTrieHash) {
			Value.empty
		} else {
			//対応する値を引いて返す。
			this.cache.get(node.hash).nodeValue
		}
	}

	private def retrieveNode2(node: TrieNode): TrieNode = {
		if (!node.isDigestNode) {
			return node
		}
		if (node.isEmpty) {
			TrieNode.empty
		} else if (node.hash == DigestUtils.EmptyTrieHash) {
			TrieNode.empty
		} else {
			//対応する値を引いて返す。
			TrieNode(this.cache.get(node.hash).nodeValue)
		}
	}

	private def putToCache(node: TrieNode): TrieNode = {
		this.cache.put(node.value) match {
			case Left(v) =>
				//値がそのままである。
				TrieNode(v)
			case Right(digest) =>
				//長かったので、ハッシュ値が返ってきたということ。
				TrieNode.fromDigest(digest)
		}
	}

	private def emptyValueSlice(i: Int): Array[Value] = {
		(0 until i).map(_ => Value.empty).toArray
	}

	/**
	 * １７要素ノードの要素を、可変の配列に変換する。
	 */
	private def copyNode(node: TrieNode): Array[Value] = {
		(0 until LIST_SIZE).map(i => Option(node.value.get(i).get).getOrElse(Value.empty)).toArray
	}

	override def sync(): Unit = {
		this.cache.commit()
		this.prevRootRef.set(root)
	}

	override def undo(): Unit = {
		this.cache.undo()
		this.rootRef.set(prevRoot)
	}

	override def validate: Boolean = Option(this.cache.get(rootHash)).isDefined

	/**
	 * このTrieに属するすべての要素を、キャッシュから削除します。
	 */
	def cleanCache(): Unit = {
		val startTime = System.currentTimeMillis

		val collectAction = new CollectFullSetOfNodes
		this.scanTree(this.rootHash, collectAction)
		val collectedHashes = collectAction.getCollectedHashes

		val cachedNodes = this.cache.getNodes
		val toRemoveSet = new mutable.HashSet[ImmutableBytes]
		for (key <- cachedNodes.keySet) {
			if (!collectedHashes.contains(key)) {
				toRemoveSet.add(key)
			}
		}
		for (key <- toRemoveSet) {
			this.cache.delete(key)
			if (logger.isTraceEnabled) {
				logger.trace("<TrieImpl> Garbage collected node: [%s]".format(key.toHexString))
			}
		}
		logger.info("<TrieImpl> Garbage collected node list, size: [%,d]".format(toRemoveSet.size))
		logger.info("<TrieImpl> Garbage collection time: [%,d ms]".format(System.currentTimeMillis - startTime))
	}

	def copy: TrieImpl = {
		val another = new TrieImpl(this.cache.dataSource, rootHash)
		this.cache.getNodes.foreach {
			each => another.cache.privatePut(each._1, each._2)
		}
		another
	}

	private def scanTree(hash: ImmutableBytes, action: ScanAction): Unit = {
		val node = this.cache.get(hash)
		if (node.nodeValue.isSeq) {
			val siblings = node.nodeValue.asSeq
			if (siblings.size == PAIR_SIZE) {
				val value = Value.fromObject(siblings(1))
				if (value.isHashCode) scanTree(value.asBytes, action)
			} else {
				(0 until LIST_SIZE).foreach {i => {
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

					this.cache.put(ImmutableBytes(key), value)
				}}
				this.root = TrieNode(Value.fromEncodedBytes(encodedRoot).decode)
			case Left(e) =>
				logger.warn("<TrieImpl> Deserialization error.", e)
		}
	}

	/**
	 * このTrieの中身をバイト列に符号化して返します。
	 * @return 符号化されたバイト列。
	 */
	def serialize: ImmutableBytes = {
		val nodes = this.cache.getNodes
		val encodedKeys = nodes.keys.foldLeft(Array.emptyByteArray)((accum, each) => accum ++ each.toByteArray)
		val encodedValues = nodes.values.map(each => RBACCodec.Encoder.encode(each.nodeValue.value))
		val encodedRoot = RBACCodec.Encoder.encode(retrieveNode(this.root).value)
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

	private val PAIR_SIZE = 2.toByte
	private val LIST_SIZE = 17.toByte

	private val EMPTY_TRIE_HASH = DigestUtils.EmptyTrieHash

	@tailrec
	def computeHash(obj: Either[ImmutableBytes, Value]): ImmutableBytes = {
		obj match {
			case null =>
				EMPTY_TRIE_HASH
			case Left(bytes) =>
				if (bytes.isEmpty) {
					EMPTY_TRIE_HASH
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
}


trait TrieNode {
	def isDigestNode: Boolean
	def isEmpty: Boolean
	def isShortcutNode: Boolean
	def shortcutKey: ImmutableBytes
	def isRegularNode: Boolean
	def value: Value
	def hash: ImmutableBytes
	def nodeValue: ImmutableBytes
	def child(idx: Int): TrieNode
}

object TrieNode {
	private val ShortcutSize = 2
	private val RegularSize = 17

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
			new ShortcutNode(value)
		} else if (value.isSeq && value.length == RegularSize) {
			new RegularNode(value)
		} else if (value.isBytes && value.asBytes.isEmpty) {
			TrieNode.empty
		} else {
			TrieNode.fromDigest(value.asBytes)
		}
	}

}

class ShortcutNode(override val value: Value) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = false
	override val isShortcutNode: Boolean = true
	override val isRegularNode: Boolean = false
	override def hash = TrieImpl.computeHash(Right(value))
	override def nodeValue: ImmutableBytes = this.value.asSeq(1).asInstanceOf[Value].asBytes
	override def shortcutKey: ImmutableBytes = this.value.asSeq.head.asInstanceOf[ImmutableBytes]
	override def child(idx: Int): TrieNode = {
		TrieNode(this.value.get(idx).get)
	}
}

class RegularNode(override val value: Value) extends TrieNode {
	override val isEmpty: Boolean = false
	override val isDigestNode: Boolean = false
	override val isShortcutNode: Boolean = false
	override val isRegularNode: Boolean = true
	override def hash = TrieImpl.computeHash(Right(value))
	override def nodeValue: ImmutableBytes = this.value.asSeq(16).asInstanceOf[Value].asBytes
	override def shortcutKey: ImmutableBytes = ImmutableBytes.empty
	override def child(idx: Int): TrieNode = {
		TrieNode(this.value.get(idx).get)
	}
}

class DigestNode(override val hash: ImmutableBytes) extends TrieNode {
	override val isEmpty: Boolean = this.hash.isEmpty
	override val isDigestNode: Boolean = true
	override val isShortcutNode: Boolean = false
	override val isRegularNode: Boolean = false
	override def value = Value.fromObject(hash)
	override def nodeValue: ImmutableBytes = throw new UnsupportedOperationException
	override def shortcutKey: ImmutableBytes = ImmutableBytes.empty
	override def child(idx: Int): TrieNode = throw new UnsupportedOperationException
}

