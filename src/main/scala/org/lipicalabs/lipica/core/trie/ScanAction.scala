package org.lipicalabs.lipica.core.trie

import org.lipicalabs.lipica.core.utils.{ImmutableBytes, Value}

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
