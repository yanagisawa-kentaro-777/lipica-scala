package org.lipicalabs.lipica.core.vm

import org.lipicalabs.lipica.core.base.Bloom
import org.lipicalabs.lipica.core.utils.RBACCodec.Decoder.DecodedResult
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, RBACCodec}

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
case class LogInfo(address: ImmutableBytes, topics: Seq[DataWord], data: ImmutableBytes) {

	def getEncoded: ImmutableBytes = {
		val encodedAddress = RBACCodec.Encoder.encode(this.address)
		val encodedTopics = this.topics.map(each => RBACCodec.Encoder.encode(each))
		val encodedData = RBACCodec.Encoder.encode(this.data)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, RBACCodec.Encoder.encodeSeqOfByteArrays(encodedTopics), encodedData))
	}

	def getBloom: Bloom = {
		var result = Bloom.create(address.digest256)
		for (eachTopic <- this.topics) {
			result = result | Bloom.create(eachTopic.computeSHA3OfData)
		}
		result
	}

	override def toString: String = {
		val topicsString = new StringBuilder
		topicsString.append("[")
		this.topics.foreach { each =>
			val eachString = each.toHexString
			topicsString.append(eachString).append(" ")
		}
		topicsString.append("]")
		"LogInfo{address=%s, topics=%s, data=%s}".format(this.address.toHexString, topicsString, this.data.toHexString)
	}

}

object LogInfo {

	def decode(items: Seq[DecodedResult]): LogInfo = {
		val address = items.head.bytes
		val topics = items(1).items.map(each => DataWord(each.bytes))
		val data = items(2).bytes
		LogInfo(address, topics, data)
	}

	def decode(encodedBytes: ImmutableBytes): LogInfo = {
		decode(RBACCodec.Decoder.decode(encodedBytes).right.get.items)
	}

}