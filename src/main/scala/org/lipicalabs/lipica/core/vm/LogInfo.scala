package org.lipicalabs.lipica.core.vm

import org.apache.commons.codec.binary.Hex
import org.lipicalabs.lipica.core.base.Bloom
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.utils.RBACCodec

/**
 *
 * @since 2015/10/24
 * @author YANAGISAWA, Kentaro
 */
case class LogInfo(address: Array[Byte], topics: Seq[DataWord], data: Array[Byte]) {

	def getEncoded: Array[Byte] = {
		val encodedAddress = RBACCodec.Encoder.encode(this.address)
		val encodedTopics = this.topics.map(each => RBACCodec.Encoder.encode(each))
		val encodedData = RBACCodec.Encoder.encode(this.data)

		RBACCodec.Encoder.encodeSeqOfByteArrays(Seq(encodedAddress, RBACCodec.Encoder.encodeSeqOfByteArrays(encodedTopics), encodedData))
	}

	def getBloom: Bloom = {
		var result = Bloom.create(DigestUtils.sha3(address))
		for (eachTopic <- this.topics) {
			result = result | Bloom.create(eachTopic.computeSHA3OfData.toByteArray)
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
		"LogInfo{address=%s, topics=%s, data=%s}".format(Hex.encodeHexString(this.address), topicsString, Hex.encodeHexString(this.data))
	}

}
