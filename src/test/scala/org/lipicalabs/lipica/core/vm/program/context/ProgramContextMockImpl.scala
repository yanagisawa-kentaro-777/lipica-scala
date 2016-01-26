package org.lipicalabs.lipica.core.vm.program.context

import org.lipicalabs.lipica.core.crypto.digest.DigestUtils
import org.lipicalabs.lipica.core.crypto.elliptic_curve.ECKeyPair
import org.lipicalabs.lipica.core.datastore.{Repository, RepositoryDummy, BlockStoreDummy, BlockStore}
import org.lipicalabs.lipica.core.kernel.Address
import org.lipicalabs.lipica.core.utils.ImmutableBytes
import org.lipicalabs.lipica.core.vm.VMWord

/**
 *
 * @since 2015/11/03
 * @author YANAGISAWA, Kentaro
 */
class ProgramContextMockImpl(private val msgData: ImmutableBytes) extends ProgramContext {

	private var _repository: Repository = new RepositoryDummy
	private var _ownerAddress = Address.parseHexString("cd2a3d9f938e13cd947ec05abc7fe734df8dd826")
	private val _contractAddress = Address.parseHexString("471fd3ad3e9eeadeec4608b92d16ce6b500704cc")
	private var _manaLimit: Long = 1000000

	this._repository.createAccount(this._ownerAddress)
	this._repository.createAccount(this._contractAddress)
	this._repository.saveCode(_contractAddress, ImmutableBytes.parseHexString("385E60076000396000605f556014600054601e60" + "205463abcddcba6040545b51602001600a525451" + "6040016014525451606001601e52545160800160" + "28525460a052546016604860003960166000f260" + "00603f556103e75660005460005360200235"))

	override def ownerAddress: VMWord = {
		VMWord(_ownerAddress.bytes)
	}

	override def balance: VMWord = {
		val balance = ImmutableBytes.parseHexString("0DE0B6B3A7640000")
		VMWord(balance)
	}

	override def originAddress: VMWord = {
		val cowPrivKey: Array[Byte] = DigestUtils.digest256("horse".getBytes)
		val addr = ECKeyPair.fromPrivateKey(cowPrivKey).toAddress
		VMWord(addr.bytes)
	}

	override def callerAddress: VMWord = {
		val cowPrivKey: Array[Byte] = DigestUtils.digest256("monkey".getBytes)
		val addr = ECKeyPair.fromPrivateKey(cowPrivKey).toAddress
		VMWord(addr.bytes)
	}

	override def manaPrice: VMWord = {
		val minManaPrice = ImmutableBytes.parseHexString("09184e72a000")
		VMWord(minManaPrice)
	}

	override def manaLimit: VMWord = {
		VMWord(_manaLimit)
	}

	def setMana(v: Long) {
		this._manaLimit = v
	}

	override def callValue: VMWord = {
		val balance= ImmutableBytes.parseHexString("0DE0B6B3A7640000")
		VMWord(balance)
	}

	override def getDataValue(indexData: VMWord): VMWord = {
		val data: Array[Byte] = new Array[Byte](32)
		val index: Int = indexData.value.intValue()
		var size: Int = 32
		if (msgData == null) return VMWord(data)
		if (index > msgData.length) return VMWord(data)
		if (index + 32 > msgData.length) size = msgData.length - index
		msgData.copyTo(index, data, 0, size)
		VMWord(data)
	}

	override def dataSize: VMWord = {
		if (msgData == null || msgData.length == 0) return VMWord(new Array[Byte](32))
		val size: Int = msgData.length
		VMWord(size)
	}

	override def getDataCopy(offsetData: VMWord, lengthData: VMWord): ImmutableBytes = {
		val offset: Int = offsetData.value.intValue()
		var length: Int = lengthData.value.intValue()
		val data: Array[Byte] = new Array[Byte](length)
		if (msgData == null) return ImmutableBytes(data)
		if (offset > msgData.length) return ImmutableBytes(data)
		if (offset + length > msgData.length) length = msgData.length - offset
		msgData.copyTo(offset, data, 0, length)
		ImmutableBytes(data)
	}

	override def parentHash: VMWord = {
		val prevHash = ImmutableBytes.parseHexString("961CB117ABA86D1E596854015A1483323F18883C2D745B0BC03E87F146D2BB1C")
		VMWord(prevHash)
	}

	override def coinbase: VMWord = {
		val coinBase = ImmutableBytes.parseHexString("E559DE5527492BCB42EC68D07DF0742A98EC3F1E")
		VMWord(coinBase)
	}

	override def timestamp: VMWord = {
		val timestamp: Long = 1401421348
		VMWord(timestamp)
	}

	override def blockNumber: VMWord = {
		val number: Long = 33
		VMWord(number)
	}

	override def difficulty: VMWord = {
		val difficulty = ImmutableBytes.parseHexString("3ED290")
		VMWord(difficulty)
	}

	override def blockManaLimit: VMWord = {
		VMWord(this._manaLimit)
	}

	def setManaLimit(v: Long) {
		this._manaLimit = v
	}

	def setOwnerAddress(ownerAddress: Address) {
		this._ownerAddress = ownerAddress
	}

	override def byTransaction: Boolean = {
		true
	}

	override def repository: Repository = {
		this._repository
	}

	override def blockStore: BlockStore = {
		new BlockStoreDummy
	}

	def setRepository(repository: Repository) {
		this._repository = repository
	}

	override def callDepth: Int = {
		0
	}
}
