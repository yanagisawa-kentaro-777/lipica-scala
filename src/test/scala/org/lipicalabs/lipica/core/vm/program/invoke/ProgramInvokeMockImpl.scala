package org.lipicalabs.lipica.core.vm.program.invoke

import org.lipicalabs.lipica.core.base.Repository
import org.lipicalabs.lipica.core.crypto.ECKey
import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.db.{RepositoryDummy, BlockStoreDummy, BlockStore}
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.DataWord

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class ProgramInvokeMockImpl(private val msgData: ImmutableBytes) extends ProgramInvoke {

	private var repository: Repository = new RepositoryDummy
	private var ownerAddress = ImmutableBytes.parseHexString("cd2a3d9f938e13cd947ec05abc7fe734df8dd826")
	private val contractAddress = ImmutableBytes.parseHexString("471fd3ad3e9eeadeec4608b92d16ce6b500704cc")
	private var manaLimit: Long = 1000000

	this.repository.createAccount(this.ownerAddress)
	this.repository.createAccount(this.contractAddress)
	this.repository.saveCode(contractAddress, ImmutableBytes.parseHexString("385E60076000396000605f556014600054601e60" + "205463abcddcba6040545b51602001600a525451" + "6040016014525451606001601e52545160800160" + "28525460a052546016604860003960166000f260" + "00603f556103e75660005460005360200235"))

	override def getOwnerAddress: DataWord = {
		DataWord(ownerAddress)
	}

	override def getBalance: DataWord = {
		val balance = ImmutableBytes.parseHexString("0DE0B6B3A7640000")
		DataWord(balance)
	}

	override def getOriginAddress: DataWord = {
		val cowPrivKey: Array[Byte] = DigestUtils.keccak256("horse".getBytes)
		val addr = ECKey.fromPrivate(cowPrivKey).getAddress
		DataWord(addr)
	}

	override def getCallerAddress: DataWord = {
		val cowPrivKey: Array[Byte] = DigestUtils.keccak256("monkey".getBytes)
		val addr = ECKey.fromPrivate(cowPrivKey).getAddress
		DataWord(addr)
	}

	override def getMinManaPrice: DataWord = {
		val minManaPrice = ImmutableBytes.parseHexString("09184e72a000")
		DataWord(minManaPrice)
	}

	override def getMana: DataWord = {
		DataWord(manaLimit)
	}

	def setMana(v: Long) {
		this.manaLimit = v
	}

	override def getCallValue: DataWord = {
		val balance= ImmutableBytes.parseHexString("0DE0B6B3A7640000")
		DataWord(balance)
	}

	override def getDataValue(indexData: DataWord): DataWord = {
		val data: Array[Byte] = new Array[Byte](32)
		val index: Int = indexData.value.intValue()
		var size: Int = 32
		if (msgData == null) return DataWord(data)
		if (index > msgData.length) return DataWord(data)
		if (index + 32 > msgData.length) size = msgData.length - index
		System.arraycopy(msgData, index, data, 0, size)
		DataWord(data)
	}

	override def getDataSize: DataWord = {
		if (msgData == null || msgData.length == 0) return DataWord(new Array[Byte](32))
		val size: Int = msgData.length
		DataWord(size)
	}

	override def getDataCopy(offsetData: DataWord, lengthData: DataWord): ImmutableBytes = {
		val offset: Int = offsetData.value.intValue()
		var length: Int = lengthData.value.intValue()
		val data: Array[Byte] = new Array[Byte](length)
		if (msgData == null) return ImmutableBytes(data)
		if (offset > msgData.length) return ImmutableBytes(data)
		if (offset + length > msgData.length) length = msgData.length - offset
		System.arraycopy(msgData, offset, data, 0, length)
		ImmutableBytes(data)
	}

	override def getLastHash: DataWord = {
		val prevHash = ImmutableBytes.parseHexString("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C")
		DataWord(prevHash)
	}

	override def getCoinbase: DataWord = {
		val coinBase = ImmutableBytes.parseHexString("E559DE5527492BCB42EC68D07DF0742A98EC3F1E")
		DataWord(coinBase)
	}

	override def getTimestamp: DataWord = {
		val timestamp: Long = 1401421348
		DataWord(timestamp)
	}

	override def getBlockNumber: DataWord = {
		val number: Long = 33
		DataWord(number)
	}

	override def getDifficulty: DataWord = {
		val difficulty = ImmutableBytes.parseHexString("3ED290")
		DataWord(difficulty)
	}

	override def getBlockManaLimit: DataWord = {
		DataWord(this.manaLimit)
	}

	def setManaLimit(v: Long) {
		this.manaLimit = v
	}

	def setOwnerAddress(ownerAddress: ImmutableBytes) {
		this.ownerAddress = ownerAddress
	}

	override def byTransaction: Boolean = {
		true
	}

	override def byTestingSuite: Boolean = {
		false
	}

	override def getRepository: Repository = {
		this.repository
	}

	override def blockStore: BlockStore = {
		new BlockStoreDummy
	}

	def setRepository(repository: Repository) {
		this.repository = repository
	}

	override def getCallDepth: Int = {
		0
	}
}
