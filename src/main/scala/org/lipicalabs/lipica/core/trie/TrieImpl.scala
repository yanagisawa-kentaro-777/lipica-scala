package org.lipicalabs.lipica.core.trie

import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.lipicalabs.lipica.core.utils.RBACCodec.Encoder
import org.lipicalabs.lipica.core.utils._
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.datasource.KeyValueDataSource
import org.slf4j.LoggerFactory

import scala.annotation.tailrec
import scala.collection.mutable

/**
 * Merkle-Patricia Treeの実装クラスです。
 */
class TrieImpl(_db: KeyValueDataSource, _root: Value) extends Trie {

	def this(_db: KeyValueDataSource) = this(_db, Value.empty)

	import TrieImpl._
	import CompactEncoder._

	/**
	 * 木構造のルート要素。
	 */
	private val rootRef = new AtomicReference[Value](_root)
	override def root(value: Value): TrieImpl = {
		this.rootRef.set(value)
		this
	}
	def root: Value = this.rootRef.get

	/**
	 * 最上位レベルのハッシュ値を計算して返します。
	 */
	override def rootHash: ImmutableBytes = {
		computeHash(Right(this.root))
	}

	@tailrec
	private def computeHash(obj: Either[ImmutableBytes, Value]): ImmutableBytes = {
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
				if (value.isBytes || value.isImmutableBytes) {
					computeHash(Left(value.asImmutableBytes))
				} else {
					value.hash
				}
		}
	}

	/**
	 * 前回のルート要素。（undo に利用する。）
	 */
	private val prevRootRef = new AtomicReference[Value](_root)
	def prevRoot: Value = this.prevRootRef.get

	/**
	 * 永続化機構のラッパー。
	 */
	private val cacheRef = new AtomicReference[Cache](new Cache(_db))
	def cache: Cache = this.cacheRef.get

	/**
	 * key 文字列に対応する値を取得して返します。
	 */
	def get(key: String): ImmutableBytes = get(ImmutableBytes(key.getBytes(StandardCharsets.UTF_8)))

	/**
	 * key に対応する値を取得して返します。
	 */
	override def get(key: ImmutableBytes): ImmutableBytes = {
		if (logger.isDebugEnabled) {
			logger.debug("Retrieving key [%s]".format(key.toHexString))
		}
		//終端記号がついた、１ニブル１バイトのバイト列に変換する。
		val convertedKey = binToNibbles(key)
		//ルートノード以下の全体を探索する。
		val found = get(this.root, convertedKey)
		found.asImmutableBytes
	}

	@tailrec
	private def get(node: Value, key: ImmutableBytes): Value = {
		if (key.isEmpty || isEmptyNode(node)) {
			//キーが消費し尽くされているか、ノードに子孫がいない場合、そのノードを返す。
			return node
		}
		val currentNode = valueOf(node)
		if (currentNode.length == PAIR_SIZE) {
			//このノードのキーを長ったらしい表現に戻す。
			val k = unpackToNibbles(currentNode.get(0).get.asImmutableBytes)
			//値を読み取る。
			val v = currentNode.get(1).get
			if ((k.length <= key.length) && key.copyOfRange(0, k.length) == k) {
				if (key.length == k.length) {
					//完全一致！
					return v
				}
				//このノードのキーが、指定されたキーの接頭辞である。
				//子孫を再帰的に探索する。
				get(v, key.copyOfRange(k.length, key.length))
			} else {
				//このノードは、指定されたキーの接頭辞ではない。
				//つまり、要求されたキーに対応する値は存在しない。
				Value.empty
			}
		} else {
			//このノードは、17要素の通常ノードである。
			//子孫をたどり、キーを１ニブル消費して探索を継続する。
			val child = currentNode.get(key(0)).get
			get(child, key.copyOfRange(1, key.length))
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
			logger.debug("Updating [%s] -> [%s]".format(key.toHexString, value.toHexString))
			logger.debug("Old root-hash: %s".format(rootHash.toHexString))
		}
		//終端記号がついた、１ニブル１バイトのバイト列に変換する。
		val nibbleKey = binToNibbles(key)
		val result = insertOrDelete(this.root, nibbleKey, value)
		//ルート要素を更新する。
		root(result)
		if (logger.isDebugEnabled) {
			logger.debug("Updated [%s] -> [%s]".format(key.toHexString, value.toHexString))
			logger.debug("New root-hash: %s".format(rootHash.toHexString))
		}
	}

	private def insertOrDelete(node: Value, key: ImmutableBytes, value: ImmutableBytes): Value = {
		if (value.nonEmpty) {
			insert(node, key, Value.fromObject(value))
		} else {
			delete(node, key)
		}
	}

	/**
	 * キーに対応する値を登録します。
	 */
	private def insert(node: Value, key: ImmutableBytes, value: Value): Value = {
		if (key.isEmpty) {
			//終端記号すらない空バイト列ということは、
			//再帰的な呼び出しによってキーが消費しつくされたということ。
			//これ以上は処理する必要がない。
			return value
		}
		if (isEmptyNode(node)) {
			//親ノードが指定されていないので、新たな２要素ノードを作成して返す。
			val newNode = Seq(packNibbles(key), value)
			return putToCache(Value.fromObject(newNode))
		}
		val currentNode = valueOf(node)
		if (currentNode.length == PAIR_SIZE) {
			//２要素のショートカットノードである。
			val packedKey = currentNode.get(0).get.asImmutableBytes
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
					insert(v, remainingKeyPart, value)
				} else {
					//既存ノードのキーの途中で分岐がある。
					//2要素のショートカットノードを、17要素の通常ノードに変換する。
					//従来の要素。
					val oldNode = insert(Value.empty, k.copyOfRange(matchingLength + 1, k.length), v)
					//追加された要素。
					val newNode = insert(Value.empty, key.copyOfRange(matchingLength + 1, key.length), value)
					//異なる最初のニブルに対応するノードを記録して、分岐させる。
					val scaledSlice = emptyValueSlice(LIST_SIZE)
					scaledSlice(k(matchingLength)) = oldNode
					scaledSlice(key(matchingLength)) = newNode
					putToCache(Value.fromObject(scaledSlice.toSeq))
				}
			if (matchingLength == 0) {
				//既存ノードのキーと新たなキーとの間に共通点はないので、
				//いま作成された通常ノードが、このノードの代替となる。
				createdNode
			} else {
				//このノードと今作られたノードとをつなぐノードを作成する。
				val bridgeNode = Seq(packNibbles(key.copyOfRange(0, matchingLength)), createdNode)
				putToCache(Value.fromObject(bridgeNode))
			}
		} else {
			//もともと17要素の通常ノードである。
			val newNode = copyNode(currentNode)
			//普通にノードを更新して、保存する。
			newNode(key(0)) = insert(currentNode.get(key(0)).get, key.copyOfRange(1, key.length), value)
			putToCache(Value.fromObject(newNode.toSeq))
		}
	}

	/**
	 * キーに対応するエントリーを削除します。
	 */
	private def delete(node: Value, key: ImmutableBytes): Value = {
		if (key.isEmpty || isEmptyNode(node)) {
			//何もしない。
			return Value.empty
		}
		val currentNode = valueOf(node)
		if (currentNode.length == PAIR_SIZE) {
			//２要素のショートカットノードである。
			//長ったらしい表現に戻す。
			val packedKey = currentNode.get(0).get.asImmutableBytes
			val k = unpackToNibbles(packedKey)

			if (k == key) {
				//ぴたり一致。 これが削除対象である。
				Value.empty
			} else if (k == key.copyOfRange(0, k.length)) {
				//このノードのキーが、削除すべきキーの接頭辞である。
				//再帰的に削除を試行する。削除した結果、新たにこのノードの直接の子になるべきノードが返ってくる。
				val deleteResult = delete(currentNode.get(1).get, key.copyOfRange(k.length, key.length))
				val newChild = valueOf(deleteResult)
				val newNode =
					if (newChild.length == PAIR_SIZE) {
						//削除で発生する跳躍をつなぐ。
						//この操作こそが、削除そのものである。
						val newKey = k ++ unpackToNibbles(newChild.get(0).get.asImmutableBytes)
						Seq(packNibbles(newKey), newChild.get(1).get)
					} else {
						Seq(packedKey, deleteResult)
					}
				putToCache(Value.fromObject(newNode))
			} else {
				//このノードは関係ない。
				node
			}
		} else {
			//もともと17要素の通常ノードである。
			val items = copyNode(currentNode)
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
					Seq(packNibbles(ImmutableBytes.fromOneByte(TERMINATOR)), items(idx))
				} else if (0 <= idx) {
					//１ノードだけ子供がいて、このノードには値がない。
					//したがって、このノードと唯一の子供とを、ショートカットノードに変換できる。
					val child = valueOf(items(idx))
					if (child.length == PAIR_SIZE) {
						val concat = ImmutableBytes.fromOneByte(idx.toByte) ++ unpackToNibbles(child.get(0).get.asImmutableBytes)
						Seq(packNibbles(concat), child.get(1).get)
					} else if (child.length == LIST_SIZE) {
						Seq(packNibbles(ImmutableBytes.fromOneByte(idx.toByte)), items(idx))
					}
				} else {
					//２ノード以上子供がいるか、子どもと値がある。
					items.toSeq
				}
			putToCache(Value.fromObject(newNode))
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

	/**
	 * 渡されたノードに内容がない場合に真を返します。
	 */
	private def isEmptyNode(node: Any): Boolean = {
		node match {
			case null =>
				true
			case _ =>
				val n = Value.fromObject(node)
				(n.isString && n.asString.isEmpty) || (n.length == 0)
		}
	}

	private def valueOf(value: Value): Value = {
		if (!value.isBytes && !value.isImmutableBytes) {
			return value
		}
		val keyBytes = value.asImmutableBytes
		if (keyBytes.isEmpty) {
			value
		} else if (keyBytes.length < 32) {
			//短いので、キーが値そのもの。
			//Value.fromObject(keyBytes)
			throw new RuntimeException
		} else {
			//長いので、対応する値を引いて返す。
			this.cache.get(keyBytes).nodeValue
		}
	}

	private def putToCache(value: Value): Value = {
		this.cache.put(value) match {
			case Left(v) =>
				//値がそのままである。
				v
			case Right(digest) =>
				//長かったので、ハッシュ値が返ってきたということ。
				Value.fromObject(digest)
		}
	}

	private def emptyValueSlice(i: Int): Array[Value] = {
		(0 until i).map(_ => Value.empty).toArray
	}

	/**
	 * １７要素ノードの要素を、可変の配列に変換する。
	 */
	private def copyNode(node: Value): Array[Value] = {
		(0 until LIST_SIZE).map(i => Option(node.get(i).get).getOrElse(Value.empty)).toArray
	}

	override def sync(): Unit = {
		this.cache.commit()
		this.prevRootRef.set(root)
	}

	override def undo(): Unit = {
		this.cache.undo()
		this.rootRef.set(this.prevRoot)
	}

	override def validate: Boolean = Option(this.cache.get(rootHash)).isDefined

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
				logger.trace("Garbage collected node: [%s]".format(key.toHexString))
			}
		}
		logger.info("Garbage collected node list, size: [%,d]".format(toRemoveSet.size))
		logger.info("Garbage collection time: [%,d ms]".format(System.currentTimeMillis - startTime))
	}

	def copy: TrieImpl = {
		val another = new TrieImpl(this.cache.dataSource, root)
		this.cache.getNodes.foreach {
			each => another.cache.privatePut(each._1, each._2)
		}
		another
	}

	private def scanTree(hash: ImmutableBytes, action: ScanAction): Unit = {
		val node = this.cache.get(hash)
		if (node eq null) return

		if (node.nodeValue.isSeq) {
			val siblings = node.nodeValue.asSeq
			if (siblings.size == PAIR_SIZE) {
				val value = Value.fromObject(siblings(1))
				if (value.isHashCode) scanTree(value.asImmutableBytes, action)
			} else {
				(0 until LIST_SIZE).foreach {i => {
					val value = Value.fromObject(siblings(i))
					if (value.isHashCode) scanTree(value.asImmutableBytes, action)
				}}
			}
			action.doOnNode(hash, node.nodeValue)
		}
	}

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

	private val EMPTY_TRIE_HASH = ImmutableBytes(DigestUtils.sha3(Encoder.encode(Array.empty[Byte])))
}