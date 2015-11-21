package org.lipicalabs.lipica.core.base.genesis

import com.google.common.io.ByteStreams
import org.codehaus.jackson.map.ObjectMapper
import org.lipicalabs.lipica.core.base.{Genesis, AccountState, BlockHeader}
import org.lipicalabs.lipica.core.config.SystemProperties
import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.utils.{ImmutableBytes, UtilConsts, JsonUtils}

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:09
 * YANAGISAWA, Kentaro
 */
object GenesisLoader {

	def loadGenesisBlock: Genesis = {
		val in = ClassLoader.getSystemResourceAsStream(SystemProperties.CONFIG.genesisInfo)
		try {
			val json = ByteStreams.toByteArray(in)
			val mapper = new ObjectMapper
			val javaType = mapper.getTypeFactory.constructType((new GenesisJson).getClass)
			val genesisJson = new ObjectMapper().readValue[GenesisJson](json, javaType)
			val genesisBlock = createGenesis(genesisJson)
			genesisBlock.stateRoot = calculateRootHash(genesisBlock.premine)
			genesisBlock
		} finally {
			in.close()
		}
	}

	private def createGenesis(genesisJson: GenesisJson): Genesis = {
		val blockHeader = new BlockHeader
		blockHeader.nonce = JsonUtils.parseHexStringToImmutableBytes(genesisJson.nonce)
		blockHeader.difficulty = JsonUtils.parseHexStringToImmutableBytes(genesisJson.difficulty)
		blockHeader.mixHash = JsonUtils.parseHexStringToImmutableBytes(genesisJson.mixhash)
		blockHeader.coinbase = JsonUtils.parseHexStringToImmutableBytes(genesisJson.coinbase)
		blockHeader.timestamp = JsonUtils.parseHexStringToLong(genesisJson.timestamp)
		blockHeader.parentHash = JsonUtils.parseHexStringToImmutableBytes(genesisJson.parentHash)
		blockHeader.extraData = JsonUtils.parseHexStringToImmutableBytes(genesisJson.extraData)
		blockHeader.manaLimit = JsonUtils.parseHexStringToLong(genesisJson.manaLimit)

		import scala.collection.JavaConversions._
		val premine = genesisJson.getAlloc.map {
			entry => {
				val (addressString, balanceString) = entry
				val address = JsonUtils.parseHexStringToImmutableBytes(addressString)
				val accountState = new AccountState(UtilConsts.Zero, BigInt(balanceString.getBalance))
				(address, accountState)
			}
		}.toMap
		new Genesis(blockHeader, premine)
	}

	private def calculateRootHash(premine: Map[ImmutableBytes, AccountState]): ImmutableBytes = {
		val trie = new SecureTrie(null)
		for (entry <- premine) {
			trie.update(entry._1, entry._2.encode)
		}
		trie.rootHash
	}

}
