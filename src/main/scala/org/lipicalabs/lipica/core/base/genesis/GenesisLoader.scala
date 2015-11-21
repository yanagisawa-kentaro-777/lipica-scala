package org.lipicalabs.lipica.core.base.genesis

import com.google.common.io.ByteStreams
import org.codehaus.jackson.map.ObjectMapper
import org.lipicalabs.lipica.core.base.Block
import org.lipicalabs.lipica.core.config.SystemProperties

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:09
 * YANAGISAWA, Kentaro
 */
object GenesisLoader {

	def loadGenesisBlock: Block = {
		val in = ClassLoader.getSystemResourceAsStream(SystemProperties.CONFIG.genesisInfo)
		try {
			val json = ByteStreams.toByteArray(in)
			val mapper = new ObjectMapper
			val javaType = mapper.getTypeFactory.constructType((new GenesisJson).getClass)
			val genesisJson = new ObjectMapper().readValue[GenesisJson](json, javaType)
			println(genesisJson.getAlloc.size())//TODO
			null
		} finally {
			in.close()
		}
	}

}
