package org.lipicalabs.lipica.core.net.transport

import java.net.{InetSocketAddress, InetAddress}
import java.util.Random

import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.net.peer_discovery.Node
import org.lipicalabs.lipica.core.net.peer_discovery.discover.table.{DistanceComparator, NodeEntry, KademliaOptions, NodeTable}
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

	"test (3)" should {
		"be right" in {
			val r = getRandom
			val table = getTestNodeTable(r, 5000)

			val closest1 = table.getClosestNodes(table.node.id)
			val closest2 = table.getClosestNodes(getNodeId(r))

			(closest1 == closest2) mustEqual false
		}
	}

//	"test (3.5)" should {
//		"be right" in {
//			val r = getRandom
//			for (i <- 0 until 1000000) {
//				val target = ImmutableBytes.createRandom(r, 64)
//				val n1 = ImmutableBytes.createRandom(r, 64)
//				val n2 = ImmutableBytes.createRandom(r, 64)
//				val node1 = NodeEntry(new Node(n1, new InetSocketAddress("127.0.0.1", 5000)))
//				val node2 = NodeEntry(new Node(n2, new InetSocketAddress("127.0.0.1", 5000)))
//				val comparator = new DistanceComparator(target)
//				val result = comparator.compare(node1, node2)
//				if (result < 0) {
//					comparator.compare(node2, node1) mustEqual 1
//				} else if (0 < result) {
//					comparator.compare(node2, node1) mustEqual -1
//				} else {
//					comparator.compare(node2, node1) mustEqual 0
//				}
//			}
//			ok
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
		new Node(getNodeId(random), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 30303))
	}

	def getNode(random: Random, id: Array[Byte], i: Int): Node = {
		id(0) = (id(0) + i).toByte
		new Node(ImmutableBytes(id), new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 30303))
	}

	def getTestNodeTable(random: Random, nodesCount: Int): NodeTable = {
		val result = new NodeTable(getNode(random))
		(0 until nodesCount).foreach(_ => result.addNode(getNode(random)))
		result
	}

}
