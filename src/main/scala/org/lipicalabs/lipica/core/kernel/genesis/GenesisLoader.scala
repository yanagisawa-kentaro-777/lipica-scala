package org.lipicalabs.lipica.core.kernel.genesis

import com.google.common.io.ByteStreams
import org.codehaus.jackson.map.ObjectMapper
import org.lipicalabs.lipica.core.kernel.{Genesis, AccountState, BlockHeader}
import org.lipicalabs.lipica.core.config.NodeProperties
import org.lipicalabs.lipica.core.trie.SecureTrie
import org.lipicalabs.lipica.core.utils._

/**
 * Created by IntelliJ IDEA.
 * 2015/11/21 11:09
 * YANAGISAWA, Kentaro
 */
object GenesisLoader {

	def loadGenesisBlock: Genesis = {
		loadGenesisBlock(NodeProperties.CONFIG.genesisResourceName)
	}

	def loadGenesisBlock(genesisFileName: String): Genesis = {
		val in = getClass.getResourceAsStream(genesisFileName)
		try {
			val json = ByteStreams.toByteArray(in)
			val mapper = new ObjectMapper
			val javaType = mapper.getTypeFactory.constructType(classOf[GenesisJson])
			val genesisJson = new ObjectMapper().readValue[GenesisJson](json, javaType)
			val genesisBlock = createGenesis(genesisJson)
			genesisBlock.stateRoot = calculateRootHash(genesisBlock.premine)
			genesisBlock
		} finally {
			Option(in).foreach(_.close())
		}
	}

	private def createGenesis(genesisJson: GenesisJson): Genesis = {
		val blockHeader = new BlockHeader
		blockHeader.nonce = JsonUtils.parseHexStringToImmutableBytes(genesisJson.nonce)
		blockHeader.difficulty = JsonUtils.parseHexStringToImmutableBytes(genesisJson.difficulty)
		blockHeader.mixHash = Digest256(JsonUtils.parseHexStringToImmutableBytes(genesisJson.mixhash))
		blockHeader.coinbase = JsonUtils.parseHexStringToImmutableBytes(genesisJson.coinbase)
		blockHeader.timestamp = JsonUtils.parseHexStringToLong(genesisJson.timestamp)
		blockHeader.parentHash = Digest256(JsonUtils.parseHexStringToImmutableBytes(genesisJson.parentHash))
		blockHeader.extraData = JsonUtils.parseHexStringToImmutableBytes(genesisJson.extraData)
		blockHeader.manaLimit = JsonUtils.parseHexStringToImmutableBytes(genesisJson.manaLimit)
		blockHeader.logsBloom = ImmutableBytes.create(256)

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

	private def calculateRootHash(premine: Map[ImmutableBytes, AccountState]): DigestValue = {
		val trie = SecureTrie.newInstance
		for (entry <- premine) {
			trie.update(entry._1, entry._2.encode)
		}
		trie.rootHash
	}

}
