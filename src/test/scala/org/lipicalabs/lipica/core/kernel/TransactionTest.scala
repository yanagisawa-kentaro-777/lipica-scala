package org.lipicalabs.lipica.core.kernel

import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.lipicalabs.lipica.core.utils.{BigIntBytes, ImmutableBytes, ByteUtils}
import org.specs2.mutable.Specification
import org.specs2.runner.JUnitRunner

/**
 * Created by IntelliJ IDEA.
 * 2015/09/08 13:01
 * YANAGISAWA, Kentaro
 */

@RunWith(classOf[JUnitRunner])
class TransactionTest extends Specification {
	sequential

	private val RLP_ENCODED_RAW_TX: String = "e88085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080"
	private val RLP_ENCODED_UNSIGNED_TX: String = "eb8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080808080"
	//private val HASH_TX: String = "328ea6d24659dec48adea1aced9a136e5ebdf40258db30d1b1d97ed2b74be34e"//TODO
	private val HASH_UNSIGNED_TX = "b747c9318ba950fb2a002683fe9d8874eb17cad6e98831f2ae08a9e5c1753710"
	private val HASH_SIGNED_TX = "5d3466b457f3480945474de8e2df3c01ceaa55a12d0347d2e17a3f3444651f86"
	private val RLP_ENCODED_SIGNED_TX: String = "f86b8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc10000801ba0eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4a014a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
	private val KEY: String = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4"
	private val testNonce: ImmutableBytes = ImmutableBytes.parseHexString("")
	private val testManaPrice: BigIntBytes = BigIntBytes(BigInt(1000000000000L))
	private val testManaLimit: BigIntBytes = BigIntBytes(BigInt(10000))
	private val testReceiveAddress: Address = Address.parseHexString("13978aee95f38490e9769c39b2773ed763d9cd5f")
	private val testValue: BigIntBytes = BigIntBytes(BigInt(10000000000000000L))
	private val testData: ImmutableBytes = ImmutableBytes.parseHexString("")
	private val testInit: ImmutableBytes = ImmutableBytes.parseHexString("")

	"test transaction from signed RBAC" should {
		"be right" in {
			val txSigned = Transaction.decode(ImmutableBytes.parseHexString(RLP_ENCODED_SIGNED_TX))
			txSigned.hash.toHexString mustEqual HASH_SIGNED_TX
			txSigned.toEncodedBytes.toHexString mustEqual RLP_ENCODED_SIGNED_TX
			txSigned.nonce.toPositiveBigInt mustEqual BigInt(0)
			txSigned.manaPrice.toPositiveBigInt mustEqual testManaPrice.toPositiveBigInt
			txSigned.manaLimit.toPositiveBigInt mustEqual testManaLimit.toPositiveBigInt
			txSigned.receiverAddress.toHexString mustEqual testReceiveAddress.toHexString
			txSigned.value.toPositiveBigInt mustEqual testValue.toPositiveBigInt
			txSigned.data.isEmpty mustEqual true
			txSigned.signatureOption.get.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(txSigned.signatureOption.get.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(txSigned.signatureOption.get.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test transaction from unsigned RBAC" should {
		"be right" in {
			val tx = Transaction.decode(ImmutableBytes.parseHexString(RLP_ENCODED_UNSIGNED_TX))
			tx.hash.toHexString mustEqual HASH_UNSIGNED_TX
			tx.toEncodedBytes.toHexString mustEqual RLP_ENCODED_UNSIGNED_TX
			tx.sign(ImmutableBytes.parseHexString(KEY))
			tx.hash.toHexString mustEqual HASH_SIGNED_TX
			tx.toEncodedBytes.toHexString mustEqual RLP_ENCODED_SIGNED_TX

			tx.nonce.toPositiveBigInt mustEqual BigInt(0)
			tx.manaPrice.toPositiveBigInt mustEqual testManaPrice.toPositiveBigInt
			tx.manaLimit.toPositiveBigInt mustEqual testManaLimit.toPositiveBigInt
			tx.receiverAddress.toHexString mustEqual testReceiveAddress.toHexString
			tx.value.toPositiveBigInt mustEqual testValue.toPositiveBigInt
			tx.data.isEmpty mustEqual true

			tx.signatureOption.get.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test new transaction (1)" should {
		"be right" in {
			val tx = Transaction(testNonce, testManaPrice, testManaLimit, testReceiveAddress, testValue, testData)

			tx.nonce.toPositiveBigInt mustEqual BigInt(0)
			tx.manaPrice.toPositiveBigInt mustEqual testManaPrice.toPositiveBigInt
			tx.manaLimit.toPositiveBigInt mustEqual testManaLimit.toPositiveBigInt
			tx.receiverAddress.toHexString mustEqual testReceiveAddress.toHexString
			tx.value.toPositiveBigInt mustEqual testValue.toPositiveBigInt
			tx.data.isEmpty mustEqual true
			tx.signatureOption.isEmpty mustEqual true

			tx.toEncodedRawBytes.toHexString mustEqual RLP_ENCODED_RAW_TX
			tx.hash.toHexString mustEqual HASH_UNSIGNED_TX
			tx.toEncodedBytes.toHexString mustEqual RLP_ENCODED_UNSIGNED_TX

			tx.sign(ImmutableBytes.parseHexString(KEY))

			tx.hash.toHexString mustEqual HASH_SIGNED_TX
			tx.toEncodedBytes.toHexString mustEqual RLP_ENCODED_SIGNED_TX
			tx.signatureOption.get.v mustEqual 27
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "14a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
		}
	}

	"test new transaction (2)" should {
		"be right" in {
			val privKeyBytes = ImmutableBytes.parseHexString("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4")
			val RLP_TX_UNSIGNED = "eb8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc1000080808080"
			val RLP_TX_SIGNED = "f86b8085e8d4a510008227109413978aee95f38490e9769c39b2773ed763d9cd5f872386f26fc10000801ba0eab47c1a49bf2fe5d40e01d313900e19ca485867d462fe06e139e3a536c6d4f4a014a569d327dcda4b29f74f93c0e9729d2f49ad726e703f9cd90dbb0fbf6649f1"
			val HASH_TX_UNSIGNED = "b747c9318ba950fb2a002683fe9d8874eb17cad6e98831f2ae08a9e5c1753710"
			val HASH_TX_SIGNED = "5d3466b457f3480945474de8e2df3c01ceaa55a12d0347d2e17a3f3444651f86"

			val nonce = ImmutableBytes.asUnsignedByteArray(BigInt(0))
			val manaPrice = BigIntBytes.parseHexString("e8d4a51000")
			val mana = BigIntBytes.parseHexString("2710")
			val receiveAddress = Address.parseHexString("13978aee95f38490e9769c39b2773ed763d9cd5f")
			val value = BigIntBytes.parseHexString("2386f26fc10000")
			val data = ImmutableBytes.empty

			val tx = Transaction(nonce, manaPrice, mana, receiveAddress, value, data)

			tx.toEncodedBytes.toHexString mustEqual RLP_TX_UNSIGNED
			tx.hash.toHexString mustEqual HASH_TX_UNSIGNED

			tx.sign(privKeyBytes)

			tx.toEncodedBytes.toHexString mustEqual RLP_TX_SIGNED
			tx.hash.toHexString mustEqual HASH_TX_SIGNED
		}
	}

	//TODO use https://github.com/ethereum/tests/blob/develop/TransactionTests/ttTransactionTest.json test cases.
	"test (1)" should {
		"be right" in {
			val tx = Transaction.decode(ImmutableBytes.parseHexString("f85f800182520894000000000000000000000000000b9331677e6ebf0a801ca098ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4aa08887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3"))

			tx.senderAddress.toHexString mustEqual "31bb58672e8bf7684108feeacf424ab62b873824"
			tx.data.isEmpty mustEqual true
			tx.manaLimit.toHexString mustEqual "5208"
			tx.manaPrice.toHexString mustEqual "01"
			tx.nonce.toHexString mustEqual ""
			tx.receiverAddress.toHexString mustEqual "000000000000000000000000000b9331677e6ebf"
			tx.value.toHexString mustEqual "0a"
			Hex.encodeHexString(Array(tx.signatureOption.get.v)) mustEqual "1c"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "98ff921201554726367d2be8c804a7ff89ccf285ebc57dff8ae4c44b9c19ac4a"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "8887321be575c8095f789dd4c743dfe42c1820f9231f98a962b210e3ac2452a3"
		}
	}

	"test (2)" should {
		"be right" in {
			val tx = Transaction.decode(ImmutableBytes.parseHexString("f86d80018259d894095e7baea6a6c7c4c2dfeb977efac326af552d870a8e0358ac39584bc98a7c979f984b031ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"))

			tx.senderAddress.toHexString mustEqual "ce26839c9bd0e87e38897bb97fca8b340fd12a53"
			tx.data.toHexString mustEqual "0358ac39584bc98a7c979f984b03"
			tx.manaLimit.toHexString mustEqual "59d8"
			tx.manaPrice.toHexString mustEqual "01"
			tx.nonce.toHexString mustEqual ""
			tx.receiverAddress.toHexString mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			tx.value.toHexString mustEqual "0a"
			Hex.encodeHexString(Array(tx.signatureOption.get.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	"test (3)" should {
		"be right" in {
			val tx = Transaction.decode(ImmutableBytes.parseHexString("f87c80018261a894095e7baea6a6c7c4c2dfeb977efac326af552d870a9d00000000000000000000000000010000000000000000000000000000001ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"))

			tx.senderAddress.toHexString mustEqual "8131688854fe0dca411aa19572a01fe3e3e4fa74"
			tx.data.toHexString mustEqual "0000000000000000000000000001000000000000000000000000000000"//TODO doubt
			tx.manaLimit.toHexString mustEqual "61a8"
			tx.manaPrice.toHexString mustEqual "01"
			tx.nonce.toHexString mustEqual ""
			tx.receiverAddress.toHexString mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			tx.value.toHexString mustEqual "0a"
			Hex.encodeHexString(Array(tx.signatureOption.get.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	"test (4)" should {
		"be right" in {
			val tx = Transaction.decode(ImmutableBytes.parseHexString("f87c80018261a894095e7baea6a6c7c4c2dfeb977efac326af552d870a9d00100000000000000000000000000000000000000000000000000000001ba048b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353a0efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"))

			tx.senderAddress.toHexString mustEqual "ead53a9560ea38feb0bc2cad8ef65e5d8f990fc1"
			tx.data.toHexString mustEqual "0010000000000000000000000000000000000000000000000000000000"//TODO doubt
			tx.manaLimit.toHexString mustEqual "61a8"
			tx.manaPrice.toHexString mustEqual "01"
			tx.nonce.toHexString mustEqual ""
			tx.receiverAddress.toHexString mustEqual "095e7baea6a6c7c4c2dfeb977efac326af552d87"
			tx.value.toHexString mustEqual "0a"
			Hex.encodeHexString(Array(tx.signatureOption.get.v)) mustEqual "1b"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.r)) mustEqual "48b55bfa915ac795c431978d8a6a992b628d557da5ff759b307d495a36649353"
			Hex.encodeHexString(ByteUtils.asUnsignedByteArray(tx.signatureOption.get.s)) mustEqual "efffd310ac743f371de3b9f7f9cb56c0b28ad43601b4ab949f53faa07bd2c804"
		}
	}

	"test (5)" should {
		"be right" in {
			val hex = "f86c05850ccc73ae708255f0944e567864ec2efad2a2186806b9afeef0caf9a427881bc16d674ec80000001ba067a47d007d52a89a3fb66d529b412f5e87e8dadbe0e9e0b3598cfaf0677645aea01f0475559fe2f9a7b9d6d7a7d7bd5b48546c2d9a41f93c8f5f03f020a91c3336"
			val tx = Transaction.decode(ImmutableBytes.parseHexString(hex))

			tx.toEncodedBytes.toHexString mustEqual hex
		}
	}

	"test(6)" should {
		"be right" in {
			val hex = "f86480850dc04240c38255f0944e567864ec2efad2a2186806b9afeef0caf9a42780001ba0de60d4a5bbef1926bb9c92e2d7e77731f728f2b1b1ed6c855f0194161ab78786a04e2c63234d8bbb822addc1f8a9054774cd8fc829aaaa998edaf2d682ef255832"
			val tx = Transaction.decode(ImmutableBytes.parseHexString(hex))
			tx.senderAddress mustEqual Address.parseHexString("4e567864ec2efad2a2186806b9afeef0caf9a427")
		}
	}

	"test various data" should {
		"be right" in {
			val seed = System.currentTimeMillis
			System.out.println("Seed=%,d".format(seed))
			val random = new java.util.Random(seed)
			(0 until 1026).foreach {i => {
				val data = ImmutableBytes(generateBytes(random, i))
				val originalTx = Transaction(testNonce, testManaPrice, testManaLimit, testReceiveAddress, testValue, data)
				val encoded = originalTx.toEncodedBytes

				val rebuiltTx = Transaction.decode(encoded)

				rebuiltTx.data mustEqual data

				rebuiltTx.nonce.toPositiveBigInt mustEqual BigInt(0)
				rebuiltTx.manaPrice.toPositiveBigInt mustEqual testManaPrice.toPositiveBigInt
				rebuiltTx.manaLimit.toPositiveBigInt mustEqual testManaLimit.toPositiveBigInt
				rebuiltTx.receiverAddress.toHexString mustEqual testReceiveAddress.toHexString
				rebuiltTx.value.toPositiveBigInt mustEqual testValue.toPositiveBigInt
				rebuiltTx.signatureOption.isEmpty mustEqual true
			}}
			ok
		}
	}

	"test various values" should {
		"be right" in {
			val seed = System.currentTimeMillis
			System.out.println("Seed=%,d".format(seed))
			Seq(0L, 1L, Int.MaxValue.toLong - 1L, Int.MaxValue.toLong, Int.MaxValue.toLong + 1L, Long.MaxValue - 1L, Long.MaxValue).foreach {eachValue => {
				val value = BigIntBytes(BigInt(eachValue))
				val originalTx = Transaction(testNonce, testManaPrice, testManaLimit, testReceiveAddress, value, testData)
				val encoded = originalTx.toEncodedBytes

				val rebuiltTx = Transaction.decode(encoded)

				rebuiltTx.value.toPositiveBigInt.toLong mustEqual eachValue

				rebuiltTx.nonce.toPositiveBigInt mustEqual BigInt(0)
				rebuiltTx.manaPrice.toPositiveBigInt mustEqual testManaPrice.toPositiveBigInt
				rebuiltTx.manaLimit.toPositiveBigInt mustEqual testManaLimit.toPositiveBigInt
				rebuiltTx.receiverAddress.toHexString mustEqual testReceiveAddress.toHexString
				rebuiltTx.data mustEqual testData
				rebuiltTx.signatureOption.isEmpty mustEqual true
			}}
			ok
		}
	}

	private def generateBytes(random: java.util.Random, length: Int): Array[Byte] = {
		val result = new Array[Byte](length)
		random.nextBytes(result)
		result
	}
}
