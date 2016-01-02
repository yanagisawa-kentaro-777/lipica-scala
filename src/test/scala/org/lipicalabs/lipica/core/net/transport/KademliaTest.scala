package org.lipicalabs.lipica.core.net.transport

import java.net.InetAddress
import java.util.Random

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.transport.discover.table.{KademliaOptions, NodeTable}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 *
 * @since 2015/12/12
 * @author YANAGISAWA, Kentaro
 */
@RunWith(classOf[JUnitRunner])
class KademliaTest extends Specification {
	sequential


	"test (1)" should {
		"be right" in {
			val r = getRandom
			val table = getTestNodeTable(r, 0)
			val homeNode = table.nodes

			Option(homeNode).isDefined mustEqual true
			table.getAllNodes.isEmpty mustEqual true
		}
	}

	"test (2)" should {
		"be right" in {
			val r = getRandom
			val table = getTestNodeTable(r, 0)
			val n = getNode(r)
			table.addNode(n)

			table.contains(n) mustEqual true

			table.dropNode(n)

			table.contains(n) mustEqual false
		}
	}

	//TODO problem
//	"test (3)" should {
//		"be right" in {
//			val r = getRandom
//			val table = getTestNodeTable(r, 5000)
//
//			val closest1 = table.getClosestNodes(table.node.id)
//			val closest2 = table.getClosestNodes(getNodeId(r))
//
//			(closest1 == closest2) mustEqual false
//		}
//	}

	"test (4)" should {
		"be right" in {
			val r = getRandom
			val table = getTestNodeTable(r, 0)
			val homeNode = table.node

			table.getBucketCount mustEqual 1

			for (i <- 1 until KademliaOptions.BucketSize) {
				table.addNode(getNode(r, homeNode.id.toByteArray, i))
			}

			1 <= table.getBucketCount mustEqual true
		}
	}

	private def getRandom: Random = {
		//val seed = 1451268255658L
		val seed = System.currentTimeMillis
		println("RNGSeed=%,d".format(seed))
		new Random(seed)
	}

	private def getNodeId(random: Random): ImmutableBytes = {
		ImmutableBytes.createRandom(random, 64)
	}

	private def getNode(random: Random): Node = {
		new Node(getNodeId(random), InetAddress.getByName("127.0.0.1"), 30303)
	}

	def getNode(random: Random, id: Array[Byte], i: Int): Node = {
		id(0) = (id(0) + i).toByte
		val n = getNode(random)
		n.id = ImmutableBytes(id)
		n
	}

	def getTestNodeTable(random: Random, nodesCount: Int): NodeTable = {
		val result = new NodeTable(getNode(random))
		(0 until nodesCount).foreach(_ => result.addNode(getNode(random)))
		result
	}

}
