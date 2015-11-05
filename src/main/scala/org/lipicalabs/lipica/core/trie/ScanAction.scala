package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, Value}

import scala.collection.immutable.HashSet
import scala.collection.mutable

/**
 *
 * @since 2015/11/05
 * @author YANAGISAWA, Kentaro
 */
trait ScanAction {
	def doOnNode(hash: ImmutableBytes, node: Value): Unit
}

class TraceAllNodes extends ScanAction {
	private val output = new StringBuilder
	override def doOnNode (hash: ImmutableBytes, node: Value): Unit = {
		output.append("%s => %s\n".format(hash.toHexString, node.toString))
	}
	def getOutput: String = output.toString()
}

class CollectFullSetOfNodes extends ScanAction {
	private val nodes = new mutable.HashSet[ImmutableBytes]
	override def doOnNode(hash: ImmutableBytes, node: Value): Unit = {
		nodes.add(hash)
	}
	def getCollectedHashes: Set[ImmutableBytes] = nodes.toSet
}
